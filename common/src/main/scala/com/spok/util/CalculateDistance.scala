package com.spok.util

trait CalculateDistance {
  def haversineDistance(pointA: (Double, Double), pointB: (Double, Double)): Double = {
    val (startpointA, endpointA) = pointA
    val (startpointB, endpointB) = pointB
    val deltaLat = math.toRadians(startpointB - startpointA)
    val deltaLong = math.toRadians(endpointB - endpointA)
    val a = math.pow(math.sin(deltaLat / 2), 2) + math.cos(math.toRadians(startpointA)) *
      math.cos(math.toRadians(startpointB)) * math.pow(math.sin(deltaLong / 2), 2)
    val greatCircleDistance = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    val distance: Double = 3958.761 * greatCircleDistance * 1609.34
    BigDecimal(distance).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble
  }
}

object CalculateDistance extends CalculateDistance
