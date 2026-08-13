package com.diploma.roadsideassistance.core

object Constants {
    // 10.0.2.2 е специалният alias, през който Android емулаторът вижда localhost
    // на машината-хост. За физическо устройство смени с LAN IP-то на компютъра,
    // напр. "http://192.168.1.50:5000/"
    const val BASE_URL = "http://10.0.2.2:5000/"

    const val DATASTORE_NAME = "roadside_assistance_prefs"
}
