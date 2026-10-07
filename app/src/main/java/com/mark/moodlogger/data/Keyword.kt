package com.mark.moodlogger.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "keywords",
    indices = [Index(value = ["name"], unique = true)],
)
data class Keyword(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(
    tableName = "entry_keywords",
    primaryKeys = ["entryId", "keywordId"],
    foreignKeys = [
        ForeignKey(
            entity = MoodEntry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Keyword::class,
            parentColumns = ["id"],
            childColumns = ["keywordId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("keywordId")],
)
data class EntryKeyword(
    val entryId: Long,
    val keywordId: Long,
)

/** A mood entry plus the keywords attached to it. */
data class EntryWithKeywords(
    @Embedded val entry: MoodEntry,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = EntryKeyword::class,
            parentColumn = "entryId",
            entityColumn = "keywordId",
        ),
    )
    val keywords: List<Keyword>,
) {
    val keywordNames: List<String> get() = keywords.map { it.name }.sorted()
}
