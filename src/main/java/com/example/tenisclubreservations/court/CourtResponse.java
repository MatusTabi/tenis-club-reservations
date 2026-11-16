package com.example.tenisclubreservations.court;

import com.example.tenisclubreservations.surface.SurfaceResponse;

public record CourtResponse(Long id, int courtNumber, SurfaceResponse surface) {
  public static CourtResponse from(Court court) {
    return new CourtResponse(
        court.getId(), court.getCourtNumber(), SurfaceResponse.from(court.getSurface()));
  }
}
