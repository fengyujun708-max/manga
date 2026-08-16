package com.mangaverse.app.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import com.mangaverse.app.core.util.ext.readEnumSet
import com.mangaverse.app.core.util.ext.readParcelableCompat
import com.mangaverse.app.core.util.ext.readSerializableCompat
import com.mangaverse.app.core.util.ext.writeEnumSet
import com.mangaverse.app.parsers.model.ContentRating
import com.mangaverse.app.parsers.model.ContentType
import com.mangaverse.app.parsers.model.Demographic
import com.mangaverse.app.parsers.model.ContentListFilter
import com.mangaverse.app.parsers.model.ContentState

object ContentListFilterParceler : Parceler<ContentListFilter> {

	override fun ContentListFilter.write(parcel: Parcel, flags: Int) {
		parcel.writeString(query)
		parcel.writeParcelable(ParcelableContentTags(tags), 0)
		parcel.writeParcelable(ParcelableContentTags(tagsExclude), 0)
		parcel.writeSerializable(locale)
		parcel.writeSerializable(originalLocale)
		parcel.writeEnumSet(states)
		parcel.writeEnumSet(contentRating)
		parcel.writeEnumSet(types)
		parcel.writeEnumSet(demographics)
		parcel.writeInt(year)
		parcel.writeInt(yearFrom)
		parcel.writeInt(yearTo)
		parcel.writeString(author)
	}

	override fun create(parcel: Parcel) = ContentListFilter(
		query = parcel.readString(),
		tags = parcel.readParcelableCompat<ParcelableContentTags>()?.tags.orEmpty(),
		tagsExclude = parcel.readParcelableCompat<ParcelableContentTags>()?.tags.orEmpty(),
		locale = parcel.readSerializableCompat(),
		originalLocale = parcel.readSerializableCompat(),
		states = parcel.readEnumSet<ContentState>().orEmpty(),
		contentRating = parcel.readEnumSet<ContentRating>().orEmpty(),
		types = parcel.readEnumSet<ContentType>().orEmpty(),
		demographics = parcel.readEnumSet<Demographic>().orEmpty(),
		year = parcel.readInt(),
		yearFrom = parcel.readInt(),
		yearTo = parcel.readInt(),
		author = parcel.readString(),
	)
}

@Parcelize
@TypeParceler<ContentListFilter, ContentListFilterParceler>
data class ParcelableContentListFilter(val filter: ContentListFilter) : Parcelable
