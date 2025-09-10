package com.redvirtualcreations.wearov.jsonObjects

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

class NullDoubleTypeAdapter : TypeAdapter<Double>() {
    override fun write(out: JsonWriter, value: Double?) {
        if(value == null) {
            out.nullValue()
            return;
        }
    }

    override fun read(reader: JsonReader): Double? {
        if(reader.peek() == JsonToken.NULL){
            reader.nextNull()
            return 0.0
        }
        val strValue = reader.nextString()
        try {
            val dval = strValue.toDouble()
            return dval;
        } catch (_ : NumberFormatException){
            return 0.0
        }
    }
}