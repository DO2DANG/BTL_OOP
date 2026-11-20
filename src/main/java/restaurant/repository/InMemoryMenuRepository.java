package restaurant.repository;

import java.util.List;
import java.util.Optional;
import restaurant.model.Beverage;
import restaurant.model.ChickenDish;
import restaurant.model.MenuCategory;
import restaurant.model.MenuItem;
import restaurant.model.SideDish;

public final class InMemoryMenuRepository implements MenuRepository {
    private static final List<MenuItem> DEFAULT_MENU = List.of(
            // Danh mục: Cơm Gà Đặc Tuyển
            new ChickenDish(1, "Cơm Gà Xối Mỡ Da Giòn", 45000, MenuCategory.COM_GA,
                    "https://i.postimg.cc/Rh6DbvtZ/com-ga-xoi-mo.jpg",
                    "Đùi gà góc tư xối mỡ vàng ươm da giòn rụm, dùng kèm cơm chiên hạt tơi đảo bơ tỏi và sốt mắm kẹo.",
                    "Xối mỡ da giòn", 10),
            new ChickenDish(2, "Cơm Gà Hải Nam Chặt Phay", 48000, MenuCategory.COM_GA,
                    "https://images.unsplash.com/photo-1598515214211-89d3c73ae83b?w=600&h=420&fit=crop",
                    "Gà ta hấp lá chanh da giòn bóng mượt, cơm nấu từ nước cốt gà béo thơm kèm sốt gừng hành đặc trưng.",
                    "Hấp lá chanh", 8),
            new ChickenDish(3, "Cơm Gà Xé Phay Hội An", 45000, MenuCategory.COM_GA,
                    "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=600&h=420&fit=crop",
                    "Cơm nghệ dẻo thơm nấu nước luộc gà, thịt gà ta xé phay trộn hành tây, rau răm và nước mạ đậm đà.",
                    "Xé phay trộn mạ", 8),
            new ChickenDish(4, "Cơm Gà Nướng Mật Ong Than Hoa", 52000, MenuCategory.COM_GA,
                    "https://images.unsplash.com/photo-1532550907401-a500c9a57435?w=600&h=420&fit=crop",
                    "Đùi gà rút xương ướp mật ong rừng nướng xém cạnh thơm lừng, ăn kèm dưa góp chua ngọt và canh gà.",
                    "Nướng than hoa", 12),
            new ChickenDish(5, "Cơm Gà Giòn Sốt Teriyaki", 46000, MenuCategory.COM_GA,
                    "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?w=600&h=420&fit=crop",
                    "Phi lê đùi gà chiên giòn rụm phủ sốt Teriyaki sánh mịn, rắc mè trắng rang thơm và hành lá tươi.",
                    "Áp chảo sốt Nhật", 10),

            // Danh mục: Món Gà Chế Biến
            new ChickenDish(6, "Gà Rán Giòn Truyền Thống", 38000, MenuCategory.MON_GA,
                    "https://i.postimg.cc/wMyGb9mC/hinh-anh-ga-ran.jpg",
                    "Lớp vỏ bột truyền thống chiên giòn tan màu vàng óng, giữ trọn thớ thịt gà mọng nước bên trong.",
                    "Chiên giòn truyền thống", 9),
            new ChickenDish(7, "Gà Rán Sốt Cay Hàn Quốc", 44000, MenuCategory.MON_GA,
                    "https://images.unsplash.com/photo-1626082927389-6cd097cdc6ec?w=600&h=420&fit=crop",
                    "Miếng gà rán giòn áo đều lớp sốt Gochujang cay ngọt nồng nàn, phủ hạt mè trắng rang thơm.",
                    "Áo sốt cay Gochujang", 11),
            new ChickenDish(8, "Cánh Gà Chiên Nước Mắm Tỏi Phi", 48000, MenuCategory.MON_GA,
                    "https://images.unsplash.com/photo-1527477396000-e27163b481c2?w=600&h=420&fit=crop",
                    "Cánh gà giữa chiên căng da, đảo nhanh cùng nước mắm nhĩ Phú Quốc và tỏi phi vàng giòn rụm.",
                    "Đảo mắm nhĩ tỏi phi", 11),
            new ChickenDish(9, "Gà Ủ Muối Hoa Tiêu (Nửa Con)", 95000, MenuCategory.MON_GA,
                    "https://images.unsplash.com/photo-1569058242253-92a9c755a0ec?w=600&h=420&fit=crop",
                    "Gà ta ủ muối thảo mộc da vàng giòn sần sật, thịt hồng mềm ngọt chấm muối tiêu chanh ớt xanh.",
                    "Ủ muối thảo mộc", 7),
            new ChickenDish(10, "Gà Viên Giòn Lắc Phô Mai", 35000, MenuCategory.MON_GA,
                    "https://images.unsplash.com/photo-1562967914-608f82629710?w=600&h=420&fit=crop",
                    "Thịt đùi gà cắt viên chiên vàng giòn rụm, lắc đều trong lớp bột phô mai cheddar béo mặn.",
                    "Chiên giòn lắc phô mai", 8),

            // Danh mục: Canh & Ăn Kèm
            new SideDish(11, "Súp Gà Nấm Hương Ngô Non", 25000,
                    "https://images.unsplash.com/photo-1547592180-85f173990554?w=600&h=420&fit=crop",
                    "Súp nấu từ nước cốt xương gà sánh nhẹ, thịt gà xé sợi, ngô ngọt và nấm hương thơm dịu.",
                    "Chén nóng", 5),
            new SideDish(12, "Khoai Tây Múi Cau Chiên Bơ", 26000,
                    "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=600&h=420&fit=crop",
                    "Khoai tây tươi bổ múi cau chiên vỏ giòn ruột bùi, xóc bơ tỏi thơm lừng ăn kèm tương ớt.",
                    "Đĩa giòn nóng", 6),
            new SideDish(13, "Khoai Lang Kén Mè Đen", 25000,
                    "https://i.postimg.cc/prh61PF3/dia-khoai-lang-ken.jpg",
                    "Khoai lang mật nghiền mịn bọc bột chiên phồng giòn rụm, dậy vị béo bùi của nước cốt dừa.",
                    "Đĩa giòn nóng", 6),

            // Danh mục: Trà & Giải Khát
            new Beverage(14, "Trà Đào Cam Sả Thanh Mát", 28000,
                    "https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=600&h=420&fit=crop",
                    "Hồng trà ủ lạnh kết hợp cam vàng, sả tươi và đào miếng giòn ngọt giúp cân bằng vị giác.",
                    true, 3),
            new Beverage(15, "Trà Tắc Xí Muội Mật Ong", 20000,
                    "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?w=600&h=420&fit=crop",
                    "Vị chua thanh của tắc tươi hòa quyện cùng xí muội mặn ngọt và mật ong rừng mát lành.",
                    true, 3),
            new Beverage(16, "Trà Thảo Mộc La Hán Quả", 22000,
                    "https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=600&h=420&fit=crop",
                    "Thức uống nấu thủ công từ la hán quả, bông cúc và táo đỏ thanh nhiệt khi dùng kèm món chiên nướng.",
                    true, 2)
    );

    private final List<MenuItem> items;

    public InMemoryMenuRepository() {
        this(DEFAULT_MENU);
    }

    public InMemoryMenuRepository(List<MenuItem> items) {
        this.items = List.copyOf(items);
    }

    @Override
    public List<MenuItem> findAll() {
        return items;
    }

    @Override
    public Optional<MenuItem> findById(int productId) {
        return items.stream().filter(item -> item.getId() == productId).findFirst();
    }
}
