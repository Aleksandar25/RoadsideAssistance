package com.diploma.roadsideassistance.util

import com.diploma.roadsideassistance.data.remote.dto.RequestStatus
import com.diploma.roadsideassistance.data.remote.dto.ServiceType

// Централизирани преводи на enum-ите на български, използвани навсякъде в UI слоя
val ServiceType.displayName: String
    get() = when (this) {
        ServiceType.TOWING -> "Репатриране"
        ServiceType.JUMP_START -> "Подаване на ток"
        ServiceType.TIRE_CHANGE -> "Спукана гума"
        ServiceType.FUEL_DELIVERY -> "Свършило гориво"
        ServiceType.MECHANICAL_FAILURE -> "Механична повреда"
        ServiceType.OTHER -> "Друго"
    }

val RequestStatus.displayName: String
    get() = when (this) {
        RequestStatus.PENDING -> "Чакаща"
        RequestStatus.ACCEPTED -> "Приета"
        RequestStatus.IN_PROGRESS -> "В ход"
        RequestStatus.COMPLETED -> "Приключена"
        RequestStatus.CANCELLED -> "Отказана"
    }
