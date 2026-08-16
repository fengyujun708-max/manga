package com.mangaverse.app.core.model.parcelable

import android.os.Parcel
import kotlinx.parcelize.Parceler
import com.mangaverse.app.core.model.ContentSource
import com.mangaverse.app.parsers.model.ContentSource

class ContentSourceParceler : Parceler<ContentSource> {

	override fun create(parcel: Parcel): ContentSource = ContentSource(parcel.readString())

	override fun ContentSource.write(parcel: Parcel, flags: Int) {
		parcel.writeString(name)
	}
}
