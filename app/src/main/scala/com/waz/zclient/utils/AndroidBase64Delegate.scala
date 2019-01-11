package com.waz.zclient.utils

import android.util.Base64
import com.waz.utils.SafeBase64

import scala.util.Try

class AndroidBase64Delegate extends SafeBase64.Delegate {
  override def encode(bytes: Array[Byte]): String = Base64.encodeToString(bytes, Base64.NO_WRAP | Base64.NO_CLOSE)
  override def decode(base64: String): Try[Array[Byte]] = Try { Base64.decode(base64, Base64.DEFAULT) }
}
