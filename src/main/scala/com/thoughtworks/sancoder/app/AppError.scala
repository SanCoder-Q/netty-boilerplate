package com.thoughtworks.sancoder

sealed trait AppError

case class LoadConfigFailed(message: String) extends AppError
