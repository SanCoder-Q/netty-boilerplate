package com.thoughtworks.sancoder.infrastructure.logging

import java.net.InetSocketAddress
import java.util.{Date, TimeZone}
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelPromise, ChannelHandlerContext, ChannelDuplexHandler}
import io.netty.handler.codec.http.HttpHeaders.Names
import io.netty.handler.codec.http.{HttpResponse, HttpRequest}
import io.netty.util.AttributeKey
import org.apache.commons.lang.time.FastDateFormat
import org.slf4j.LoggerFactory
import scalaz._, Scalaz._

@Sharable
object AccessLoggingChannelHandler extends ChannelDuplexHandler {

  private val accessLog = LoggerFactory.getLogger("access")

  private val accessLogAttributeKey: AttributeKey[Option[AccessLogRequestInfo]] = AttributeKey.valueOf("accessLog")

  override def channelRead(ctx: ChannelHandlerContext, msg: AnyRef): Unit = {
    val accessLogAttribute = ctx.attr(accessLogAttributeKey)

    val requestInfoOption = newAccessLogRequestInfo(ctx, msg)
    accessLogAttribute.set(requestInfoOption)
    super.channelRead(ctx, msg)
  }

  private def newAccessLogRequestInfo(ctx: ChannelHandlerContext, msg: AnyRef): Option[AccessLogRequestInfo] = {
    if (msg.isInstanceOf[HttpRequest]) {
      val request = msg.asInstanceOf[HttpRequest]
      val headers = request.headers
      val startTime = System.currentTimeMillis()

      def header(name: String): Option[String] = Option(headers.get(name))

      Some(AccessLogRequestInfo(startTime, remoteAddress(ctx), request.getMethod.name, request.getUri,
              request.getProtocolVersion.text, header(Names.REFERER),
              header(Names.USER_AGENT), header(Names.HOST), header("X-Forwarded-For")))
    } else None
  }

  private def remoteAddress(ctx: ChannelHandlerContext): Option[String] = {
    val address = ctx.channel().remoteAddress()
    // on the off chance that this isn't an ip connection, we're going to check first
    if (address.isInstanceOf[InetSocketAddress]) {
      val inetAddress = address.asInstanceOf[InetSocketAddress].getAddress().toString()
      if (inetAddress.startsWith("/")) Some(inetAddress.substring(1)) else Some(inetAddress)
    } else None
  }

  override def write(ctx: ChannelHandlerContext, msg: AnyRef, promise: ChannelPromise): Unit = {
    super.write(ctx, msg, promise)


    if (msg.isInstanceOf[HttpResponse]) {
      val response = msg.asInstanceOf[HttpResponse]
      val headers = response.headers()

      val accessLogAttribute = ctx.attr(accessLogAttributeKey)
      val requestInfoOption = accessLogAttribute.get()
      requestInfoOption.foreach { requestInfo =>
        // TODO What if access log info was None or msg wasn't a HttpResponse
        val accessLogInfo = AccessLogInfo(requestInfo, response.getStatus.code,
          Option(headers.get(Names.CONTENT_LENGTH)).flatMap(_.parseLong.toOption),
          System.currentTimeMillis - requestInfo.startTime)
        accessLog.info(AppAccessLogFormatter.format(accessLogInfo))
      }
    }
  }
}

case class AccessLogRequestInfo(startTime: Long, remoteAddress: Option[String], method: String, uri: String,
                                httpVersion: String, referer: Option[String], userAgent: Option[String],
                                host: Option[String], xForwardedFor: Option[String])

case class AccessLogInfo(request: AccessLogRequestInfo, statusCode: Int, contentLength: Option[Long], responseTime: Long)

object AppAccessLogFormatter extends LogFormatter {

  private val dateFormat = FastDateFormat.getInstance("dd/MMM/yyyy:HH:mm:ss Z", TimeZone.getTimeZone("GMT"))

  /*
   * This format was extended from the com.twitter.finagle.http.filter.CommonLogFormatter
   *
   * Apache common log format is: "%h %l %u %t \"%r\" %>s %b"
   *   %h: remote host
   *   %l: remote logname
   *   %u: remote user
   *   %t: time request was received
   *   %r: request lime
   *   %s: status
   *   %b: bytes
   *
   * We've added a number of other details:
   *   %D: response time in milliseconds
   *   "%{Referer}i": referer URL
   *   "%{User-Agent}i": user agent
   *   "%{Host}i": host header
   *   "%{X-Forwarded-For}i": X-Forwarded-For origin IP address
   */
  def format(accessLogInfo: AccessLogInfo): String = {

    val builder = new StringBuilder
    builder.append(orDefault(accessLogInfo.request.remoteAddress))
    builder.append(" - - [")
    builder.append(formattedDate)
    builder.append("] \"")
    builder.append(escape(accessLogInfo.request.method))
    builder.append(' ')
    builder.append(escape(accessLogInfo.request.uri))
    builder.append(' ')
    builder.append(escape(accessLogInfo.request.httpVersion))
    builder.append("\" ")
    builder.append(accessLogInfo.statusCode.toString)
    builder.append(' ')
    builder.append(orDefault(accessLogInfo.contentLength.filter(_ > 0).map(_.toString)))
    builder.append(' ')
    builder.append(accessLogInfo.responseTime)
    builder.append(" \"")
    builder.append(escape(orDefault(accessLogInfo.request.referer)))
    builder.append('"')
    builder.append(" \"")
    builder.append(escape(orDefault(accessLogInfo.request.userAgent)))
    builder.append('"')
    builder.append(" \"")
    builder.append(escape(orDefault(accessLogInfo.request.host)))
    builder.append('"')
    builder.append(" \"")
    builder.append(escape(orDefault(accessLogInfo.request.xForwardedFor)))
    builder.append('"')

    builder.toString
  }

  private def formattedDate(): String = dateFormat.format(new Date())

  private def orDefault(optional: Option[String]) = optional.getOrElse("-")

}

// Copied as is from Finagle source code
// https://github.com/twitter/finagle/blob/1e06db17ca2de4b85209dd2fbc18e635815e994b/finagle-http/src/main/scala/com/twitter/finagle/http/filter/LoggingFilter.scala#L20-L64
trait LogFormatter {
  private val BackslashV = 0x0b.toByte

  /** Escape string for logging (compatible with Apache's ap_escape_logitem()) */
  def escape(s: String): String = {
    var builder: StringBuilder = null // only create if escaping is needed
    var index = 0
    s.foreach { c =>
      val i = c.toInt
      if (i >= 0x20 && i <= 0x7E && i != 0x22 && i != 0x5C) {
        if (builder == null) {
          index += 1 // common case
        } else {
          builder.append(c)
        }
      } else {
        if (builder == null) {
          builder = new StringBuilder(s.substring(0, index))
        }
        c match {
          case '\b'       => builder.append("\\b")
          case '\n'       => builder.append("\\n")
          case '\r'       => builder.append("\\r")
          case '\t'       => builder.append("\\t")
          case BackslashV => builder.append("\\v")
          case '\\'       => builder.append("\\\\")
          case '"'        => builder.append("\\\"")
          case _ =>
            c.toString().getBytes("UTF-8").foreach { byte =>
              builder.append("\\x")
              val s = java.lang.Integer.toHexString(byte & 0xff)
              if (s.length == 1)
                builder.append("0")
              builder.append(s)
            }
        }
      }
    }
    if (builder == null) {
      s // common case: nothing needed escaping
    } else {
      builder.toString
    }
  }
}
