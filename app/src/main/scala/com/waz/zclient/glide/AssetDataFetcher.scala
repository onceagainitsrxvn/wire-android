/**
 * Wire
 * Copyright (C) 2018 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.zclient.glide

import java.io.InputStream

import android.content.Context
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.waz.ZLog
import com.waz.ZLog.ImplicitTag.implicitLogTag
import com.waz.service.assets.AssetService.BitmapResult.{BitmapLoaded, LoadingFailed}
import com.waz.ui.MemoryImageCache.BitmapRequest.Regular
import com.waz.utils.wrappers.AndroidBitmap
import com.waz.zclient.common.views.ImageController
import com.waz.zclient.{Injectable, Injector}

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class AssetDataFetcher(request: AssetRequest, width: Int)(implicit context: Context, inj: Injector) extends DataFetcher[InputStream] with Injectable {

  private lazy val imageController = inject[ImageController]

  private lazy val bitmapSignal = (request match {
    case AssetIdRequest(assetId) => imageController.imageSignal(assetId, Regular(width), forceDownload = true)
    case AssetDataRequest(assetData) => imageController.imageSignal(assetData, Regular(width), forceDownload = true)
  }).collect {
    case BitmapLoaded(AndroidBitmap(bm), _) => Right(bm)
    case LoadingFailed(e: Exception) => Left(e)
  }.disableAutowiring()

  override def loadData(priority: Priority, callback: DataFetcher.DataCallback[_ >: InputStream]): Unit = {

    ZLog.verbose(s"loadData $request")

    Await.result(bitmapSignal.head, Duration.Inf) match {
      case Left(e) =>
        ZLog.verbose(s"bitmapSignal failed $request")
        callback.onLoadFailed(e)
      case Right(bmp) =>
        ZLog.verbose(s"bitmapSignal success $request")

        import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

        import android.graphics.Bitmap.CompressFormat

        val bos = new ByteArrayOutputStream()
        bmp.compress(CompressFormat.PNG, 0 , bos)
        val bitmapData = bos.toByteArray
        val is = new ByteArrayInputStream(bitmapData)
        callback.onDataReady(is)
    }
  }

  override def cleanup(): Unit = {}

  override def cancel(): Unit = {}

  override def getDataClass: Class[InputStream] = classOf[InputStream]

  override def getDataSource: DataSource = DataSource.REMOTE
}