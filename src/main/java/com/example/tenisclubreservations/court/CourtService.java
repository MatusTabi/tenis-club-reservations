package com.example.tenisclubreservations.court;

import com.example.tenisclubreservations.common.ApiException;
import com.example.tenisclubreservations.common.Input;
import com.example.tenisclubreservations.common.MutationGuard;
import com.example.tenisclubreservations.surface.Surface;
import com.example.tenisclubreservations.surface.SurfaceDao;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CourtService {
  private final CourtDao courts;
  private final SurfaceDao surfaces;
  private final MutationGuard guard;

  private Court find(long id) {
    return courts
        .find(id)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Court not found"));
  }

  @Transactional(readOnly = true)
  public CourtResponse get(long id) {
    return CourtResponse.from(find(id));
  }

  @Transactional(readOnly = true)
  public List<CourtResponse> list() {
    return courts.list().stream().map(CourtResponse::from).toList();
  }

  public CourtResponse create(CourtRequest request) {
    guard.lock();
    Court court = new Court();
    updateValues(court, request.fields(), false);
    courts.persist(court);
    return CourtResponse.from(court);
  }

  public CourtResponse update(long id, CourtRequest request, boolean partial) {
    guard.lock();
    Court court = find(id);
    updateValues(court, request.fields(), partial);
    return CourtResponse.from(court);
  }

  private void updateValues(Court court, Map<String, Object> input, boolean partial) {
    Input.fields(input, Set.of("courtNumber", "surfaceId"), partial);
    long number =
        input.containsKey("courtNumber")
            ? Input.positiveInteger(input.get("courtNumber"))
            : court.getCourtNumber();
    if (number > Integer.MAX_VALUE) {
      throw Input.invalid("Court number is too large");
    }
    Surface surface =
        input.containsKey("surfaceId")
            ? surfaces
                .find(Input.positiveInteger(input.get("surfaceId")))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Surface not found"))
            : court.getSurface();
    if (courts.exists((int) number, court.getId())) {
      throw new ApiException(HttpStatus.CONFLICT, "Court number already exists");
    }
    court.update((int) number, surface);
  }

  public void delete(long id) {
    guard.lock();
    find(id).softDelete();
  }
}
