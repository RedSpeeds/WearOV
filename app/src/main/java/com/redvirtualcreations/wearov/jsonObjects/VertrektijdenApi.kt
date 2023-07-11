package com.redvirtualcreations.wearov.jsonObjects

import com.google.gson.annotations.SerializedName

data class VertrektijdenApi(
    @SerializedName("TRAIN" ) var TRAIN : ArrayList<TRAIN> = arrayListOf(),
    @SerializedName("BTMF"  ) var BTMF  : ArrayList<BTMF>  = arrayListOf(),
    var apiError : Boolean = false
)

data class StationInfo (

    @SerializedName("StopCode"  ) var StopCode  : String = "",
    @SerializedName("StopName"  ) var StopName  : String = "",
    @SerializedName("Town"      ) var Town      : String = "",
    @SerializedName("Longitude" ) var Longitude : Double = 0.0,
    @SerializedName("Latitude"  ) var Latitude  : Double = 0.0,
    @SerializedName("Distance"  ) var Distance  : Double = 0.0

)
data class DepartureTrain (

    @SerializedName("Destination"       ) var Destination       : String           = "",
    @SerializedName("TransportType"     ) var TransportType     : String           = "",
    @SerializedName("TransportTypeCode" ) var TransportTypeCode : String           = "",
    @SerializedName("Agency"            ) var Agency            : String           = "",
    @SerializedName("PlannedDeparture"  ) var PlannedDeparture  : String           = "",
    @SerializedName("Via"               ) var Via               : String           = "",
    @SerializedName("Tips"              ) var Tips              : ArrayList<String> = arrayListOf(),
    @SerializedName("Comments"          ) var Comments          : ArrayList<String> = arrayListOf(),
    @SerializedName("Delay"             ) var Delay             : Int              = 0,
    @SerializedName("Platform"          ) var Platform          : String           = "",
    @SerializedName("PlatformChange"    ) var PlatformChange    : Boolean          = false

)
data class DepartureBTMF (

    @SerializedName("LineNumber"           ) var LineNumber           : String = "",
    @SerializedName("LineName"             ) var LineName             : String = "",
    @SerializedName("Destination"          ) var Destination          : String = "",
    @SerializedName("DestinationCode"      ) var DestinationCode      : String = "",
    @SerializedName("TransportType"        ) var TransportType        : String = "",
    @SerializedName("AgencyCode"           ) var AgencyCode           : String = "",
    @SerializedName("UpdateTime"           ) var UpdateTime           : String = "",
    @SerializedName("PlannedDeparture"     ) var PlannedDeparture     : String = "",
    @SerializedName("ExpectedDeparture"    ) var ExpectedDeparture    : String = "",
    @SerializedName("VehicleStatus"        ) var VehicleStatus        : String = "",
    @SerializedName("StationStatus"        ) var StationStatus        : String = "",
    @SerializedName("WheelchairAccessible" ) var WheelchairAccessible : String = "",
    @SerializedName("LinePlanningNumber"   ) var LinePlanningNumber   : String = "",
    @SerializedName("JourneyNumber"        ) var JourneyNumber        : Int    = 0,
    @SerializedName("Platform"             ) var Platform             : String = ""

)

data class TRAIN (

    @SerializedName("Station_Info" ) var StationInfo : StationInfo          = StationInfo(),
    @SerializedName("Departures"   ) var Departures  : ArrayList<DepartureTrain> = arrayListOf()

)
data class BTMF (

    @SerializedName("Station_Info" ) var StationInfo : StationInfo          = StationInfo(),
    @SerializedName("Departures"   ) var Departures  : ArrayList<DepartureBTMF> = arrayListOf()

)