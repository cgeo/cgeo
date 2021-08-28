## Bugfix Release

### Design
- Increase font size for text input fields
- Increase font size for some compass elements
- Use font color with higher contrast in waypoint tab
- Make quick offline log check mark visible again
- Increase font size for coordinate input fields
- Respect system font size settings also on older Android versions (5,6 and 7)

### Geocache Ayrıntıları
- Fix missing cache title if cache opened via geocode or link (website change)
- Fix missing cache description on some caches

### Diğer
- Show premium caches again in search results of basic members
- Fix further creation of user defined caches if some user defines caches have been loaded via GPX
- Use more common English abbreviation for traditional cache in cache type filter

## Feature Release 2021.08.15:

### Gelişmiş filtreleme sistemi
- c:geo için esnek, birleştirilebilir ve depolanabilir filtreleri destekleyen yeni bir filtreleme sisteminin tanıtılıyor
- Hem cache listelerinde hem de harita görünümünde mevcuttur
- Yeni "Filtreye göre ara" işlevi

### Harita
- Yeni: Bir listeden bir harita görüntülerken kullanıcı tanımlı bir cache oluşturma: Kullanıcıya mevcut listede (kullanıcı tanımlı cacheler için varsayılan liste yerine) yeni cache saklamasını sağlar
- Yeni: Harita hızlı ayarlarında "kendi" ve "bulunan" filtreleri ayrıldı
- Değişiklik: Ek olarak, açılır pencere ayrıntılarında cache adını göster

### Geocache Ayrıntıları
- Yeni: Google translate uygulama içi çeviri açılır penceresini kullanın
- Yeni: Cache ayrıntıları açılır penceresindeki atanan simgeyi uzun tıklamayla değiştirmeye izin ver (yalnızca depolanan cacheler)

### İndirici
- Değişiklik: İndirmeler artık tamamen arka planda gerçekleşecek, bildirim gönderilecek
- Değişiklik: Başarıyla indirilen dosyalar, aynı ada sahip mevcut dosyaların üzerine otomatik olarak yazılacaktır
- Değişiklik: Bir harita henüz yüklenmemiş belirli bir tema gerektiriyorsa, c:geo o temayı da otomatik olarak indirecek ve kuracaktır

### Diğer
- Değişiklik: Android tarafından sağlanan daha modern bileşenlerden faydalanabilmek için c:geo temasının dahili teknik yönlerini tamamen elden geçirdik. Bunun, bazıları istenmeyen birkaç yan etkisi olacaktır. Lütfen hataları veya aksaklıkları [GitHub sayfamızdan](https://www.github.com/cgeo/cgeo/issues) veya destek ekibiyle iletişime geçerek bildirin.
- Yeni: Sistemden gece / gündüz modunu destekler (isteğe bağlı)
- Yeni: Geocaching.com'dan yer imi listelerini indirin - ana menüde "Listeler / cep sorguları"na bakın
- Yeni: geocaching.su için yeteneği yoksay
- Değişiklik: Artık bakımı yapılmayan RMAPS navigasyon uygulaması kaldırıldı
- Düzeltme: Kişisel nottan aynı ada sahip ancak farklı koordinatlara sahip yol noktasını çıkarma
- Düzeltme: Formüllü yol noktası için kullanıcı notunun çıkarılmasında hata
- Düzeltme: Formülü tamamlanmış formül için koordinatlar yerine KN'ye aktar
- Düzeltme: Yeniden yüklemeden ve yedek geri yüklemeden sonra çevrimdışı harita ve tema klasörü hatası
- Düzeltme: Yol/rota güncellenemiyor
- Düzeltme: Açık temada indirici için tema oluşturma hatası
