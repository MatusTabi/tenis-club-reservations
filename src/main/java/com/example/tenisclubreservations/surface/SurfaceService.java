package com.example.tenisclubreservations.surface;

import com.example.tenisclubreservations.common.ApiException;
import com.example.tenisclubreservations.common.Input;
import com.example.tenisclubreservations.common.MutationGuard;
import com.example.tenisclubreservations.court.CourtDao;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SurfaceService {
  private final SurfaceDao surfaces;
  private final CourtDao courts;
  private final MutationGuard guard;

  public Surface getEntity(long id) {
    return surfaces
        .find(id)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Surface not found"));
  }

  @Transactional(readOnly = true)
  public SurfaceResponse get(long id) {
    return SurfaceResponse.from(getEntity(id));
  }

  @Transactional(readOnly = true)
  public List<SurfaceResponse> list() {
    return surfaces.list().stream().map(SurfaceResponse::from).toList();
  }

  public SurfaceResponse create(SurfaceRequest request) {
    guard.lock();
    Surface surface = new Surface();
    updateValues(surface, request.fields(), false);
    surfaces.persist(surface);
    return SurfaceResponse.from(surface);
  }

  public SurfaceResponse update(long id, SurfaceRequest request, boolean partial) {
    guard.lock();
    Surface surface = getEntity(id);
    updateValues(surface, request.fields(), partial);
    return SurfaceResponse.from(surface);
  }

  private void updateValues(Surface surface, Map<String, Object> input, boolean partial) {
    Input.fields(input, Set.of("name", "pricePerMinute"), partial);
    String name = input.containsKey("name") ? Input.name(input.get("name")) : surface.getName();
    BigDecimal price =
        input.containsKey("pricePerMinute")
            ? Input.price(input.get("pricePerMinute"))
            : surface.getPricePerMinute();
    String key = name.toLowerCase(Locale.ROOT);
    if (surfaces.exists(key, surface.getId())) {
      throw new ApiException(HttpStatus.CONFLICT, "Surface name already exists");
    }
    surface.update(name, key, price);
  }

  public void delete(long id) {
    guard.lock();
    Surface surface = getEntity(id);
    if (courts.assigned(id)) {
      throw new ApiException(HttpStatus.CONFLICT, "Surface is assigned to a court");
    }
    surface.softDelete();
  }
}
