package database

import androidx.room.TypeConverter
import java.util.Date


class CrimeTypeConverter {

    @TypeConverter
    fun fromDate(date: Date) : Long{
        return date.time
    }

    @TypeConverter
    fun toDate (millisSinceEpoch : Long) : Date {
        return Date(millisSinceEpoch)
    }

}