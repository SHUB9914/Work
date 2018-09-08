package com.spok.model

case class AddressComponents(long_name: String, short_name: String, types: List[String])

case class NorthEast(lat: Double, lng: Double)

case class SouthWest(lat: Double, lng: Double)

case class Bounds(northeast: NorthEast, southwest: SouthWest)

case class InnerLocation(lat: Double, lng: Double)

case class ViewPort(northeast: NorthEast, southwest: SouthWest)

case class Geometry(bounds: Bounds, location: InnerLocation, location_type: String, viewport: ViewPort)

case class LocationDetails(address_components: List[AddressComponents], formatted_address: String,
  geometry: Geometry, place_id: String, types: List[String])

case class Location(results: List[LocationDetails], status: String)
