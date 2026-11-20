package restaurant.repository;

import java.util.List;
import java.util.Optional;
import restaurant.model.MenuItem;

public interface MenuRepository {
    List<MenuItem> findAll();

    Optional<MenuItem> findById(int productId);
}
