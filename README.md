# Sổ tay thu chi — bản cơ bản 1.10

Menu hiện tại: **Trang chủ – Giao dịch – ＋ – Summary – Tài khoản**. Trang chủ hiển thị biểu đồ tròn theo danh mục, bộ chọn tháng/loại Thu–Chi, tên và avatar. Summary hiển thị số dư lũy kế, thu/chi tháng, biểu đồ cột và các thao tác trước đây ở Trang chủ. Hai tab giữ riêng tháng đang xem khi đổi tab.

Mở **Tiết kiệm · Mục tiêu của bạn** từ Summary hoặc Tài khoản. Lịch sử tiết kiệm quay lại đúng trang Tiết kiệm, rồi về trang đã mở nó. Biểu đồ tròn dùng cùng bán kính theo hai chiều, nét tròn đều, đầu đoạn màu bo tròn và khoảng cách nhỏ; vẫn chọn được danh mục và xem số tiền/tỷ lệ chính xác.

Toàn ứng dụng dùng nền navy tối như Summary: thẻ bo góc, menu kính blur, nút, form thêm/sửa, trang tài khoản và các bảng chi tiết. Chữ sáng; thu nhập xanh mint, chi tiêu hồng. Hộp chọn ngày và danh mục cũng theo giao diện tối. Dữ liệu thu/chi, mục tiêu, tên và avatar được giữ nguyên khi cài cập nhật.

Kiểm chứng bản 1.10: build APK và lint thành công (còn cảnh báo); 3 kiểm thử JVM và 40 kiểm thử Android đều qua trên máy giả lập API 36.1. Có kiểm thử pixel xác nhận vòng tròn cân bằng hai chiều và ở giữa khung hình, kiểm thử chọn đoạn/danh mục, giữ tháng/loại khi đổi tab, điều hướng Tiết kiệm và hồ sơ/avatar. Chưa chạy trên điện thoại thật.

Quản lý danh mục tại **Tài khoản → Danh mục chi tiêu / Danh mục thu nhập**: thêm tên mới (tối đa 40 ký tự), đổi tên, xóa. Đổi tên cập nhật cả giao dịch và lịch định kỳ. Xóa danh mục đang dùng yêu cầu chọn danh mục thay thế; không xóa giao dịch hoặc thay đổi số tiền. Không cho tên trùng (không phân biệt chữ hoa/thường), tên rỗng hoặc xóa danh mục cuối cùng. Hai loại có danh sách riêng; có thể dùng cùng một tên ở cả hai loại. Thu nhập có sẵn Lương, Thưởng, Kinh doanh, Đầu tư, Quà tặng và Thu nhập khác. Các khoản thu cũ thuộc nhóm “Thu nhập” được giữ nguyên.

Danh sách tùy chỉnh được lưu trong SQLite và dùng chung cho nhập giao dịch, lịch định kỳ, bộ lọc và báo cáo. Nâng cấp từ bản cũ giữ dữ liệu và nhập các danh mục đã có trong giao dịch. Giao diện mới: `screen_categories.xml` và `dialog_category.xml`.

Ứng dụng Android Java, giao diện XML, lưu dữ liệu SQLite trên máy.

## Xem màn hình trong Android Studio

Mở đúng thư mục `E:\AndroidStudioProjects\QuanLyThuChi`, chờ Gradle Sync hoàn tất. Mở `app/src/main/res/layout`, chọn tệp rồi chọn **Design** hoặc **Split**:

| Tệp XML | Màn hình |
| --- | --- |
| `activity_main.xml` | Khung ứng dụng, thanh điều hướng và Trang chủ |
| `screen_summary.xml` | Trang chủ: biểu đồ tròn, tổng tháng và danh mục thu/chi |
| `screen_home.xml` | Tab Summary: số dư, thu chi, biểu đồ cột, ngân sách và lối vào Tiết kiệm |
| `screen_transactions.xml` | Tab Giao dịch: tất cả giao dịch, tìm kiếm và lọc tháng/danh mục |
| `screen_reports.xml` | Báo cáo trong Tài khoản: năm, 12 tháng, biểu đồ cột Thu/Chi có thể chạm và so sánh |
| `screen_savings_history.xml` | Lịch sử góp/rút tiền tiết kiệm |
| `screen_savings.xml` | Mục tiêu tiết kiệm |
| `screen_account.xml` | Hồ sơ và ngân sách |
| `screen_recurring.xml` | Giao dịch định kỳ hàng tháng |
| `dialog_transaction.xml` | Form thêm/sửa khoản thu hoặc chi |

Đây là các layout thực sự được dùng khi chạy. Dữ liệu danh sách được điền từ SQLite lúc chạy. `res/drawable/ic_wallet.xml` chỉ là biểu tượng ứng dụng.

## Chức năng hiện tại

- Thêm, sửa, xóa thu/chi: số tiền nguyên VNĐ, ghi chú, ngày, danh mục chọn thủ công.
- Tổng thu/chi, chênh lệch và ngân sách theo tháng; tìm kiếm ghi chú, lọc danh mục.
- Báo cáo 12 tháng, biểu đồ cột Thu/Chi, so sánh tháng và so với năm trước. Chạm cột để xem đúng giao dịch của tháng và loại thu/chi đó.
- Mục tiêu tiết kiệm: tạo/sửa/xóa, góp/rút tiền, tiến độ.
- Lịch thu/chi hàng tháng: ghi nhận khoản đến hạn khi mở app, chống trùng và hỗ trợ cuối tháng.
- Chỉnh tên hồ sơ và ngân sách. Chưa có đăng nhập/đồng bộ máy chủ.

Tiền góp mục tiêu là phân bổ riêng, không tự tạo khoản chi. Dữ liệu thật khởi đầu trống. Dữ liệu lưu trong ứng dụng; gỡ app sẽ mất dữ liệu. 

## Đã bỏ theo yêu cầu

Màn Gợi ý, LLM, phân tích AI, tính kế hoạch vay, tự phân loại, đọc thông báo ngân hàng, cấu hình máy chủ và đọc tệp sao lưu. Không còn quyền Internet, quyền đọc dữ liệu điện thoại hoặc NotificationListenerService. Đã bỏ xuất Excel; không xin quyền bộ nhớ.

Cập nhật từ bản 1.0 giữ giao dịch, lịch định kỳ, mục tiêu và hồ sơ; xóa hàng chờ thông báo và cấu hình kết nối cũ. Application ID và khóa debug giữ nguyên để có thể cài đè bản trước.

## Build và cài trên điện thoại

Yêu cầu JDK 17 trở lên, SDK 36; minSdk 26. Chạy trong Terminal tại thư mục dự án:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

APK build: `app/build/outputs/apk/debug/app-debug.apk`. Bản giao được chép thành `SoTayThuChi.apk` tại thư mục dự án. Chuyển APK sang điện thoại Android và mở để cài. Đây là bản debug, chưa phải bản phát hành Google Play.

Để chạy trong Android Studio, chọn cấu hình **ứng dụng app**, không chọn `androidTest`, rồi chọn điện thoại/máy ảo. Nếu chưa có cấu hình ứng dụng, tạo **Android App** trong Run → Edit Configurations, chọn module chính có `MainActivity`, Launch: Default Activity.

Kiểm thử gồm số tiền, ngày cuối tháng, chọn cột biểu đồ, chi tiết số dư, SQLite, chuyển màn XML, loại bỏ quyền truy cập và nâng cấp dữ liệu từ bản trước.

## Điều hướng và số dư phiên bản 1.3

Menu: Trang chủ – Giao dịch – ＋ – Tiết kiệm – Tài khoản. Nút ＋ mở form thêm thu/chi từ bất kỳ tab nào. Báo cáo nằm trong Tài khoản.

Trang chủ hiển thị thu và chi riêng của tháng được chọn; số dư lũy kế bằng số dư tất cả tháng trước cộng thu tháng này trừ chi tháng này. Không tạo giao dịch giả để chuyển số dư, nên sửa/xóa giao dịch cũ tự cập nhật các tháng sau; số âm cũng được chuyển tiếp.

Tiết kiệm → Lịch sử tiết kiệm ghi lại từng lần góp/rút với thời gian, mục tiêu và số dư sau giao dịch. Số tiền tiết kiệm có từ bản cũ được đánh dấu Số dư từ bản trước, không giả định ngày góp tiền. Xóa mục tiêu giữ lịch sử. Góp tiết kiệm vẫn là phân bổ riêng, không trừ hai lần vào thu/chi.

## Giao diện phiên bản 1.4

Nền xanh ngọc/tím với lớp màu làm mờ được lưu sẵn, các thẻ kính bán trong suốt và menu nổi bo góc. Chữ và nút luôn giữ sắc nét.

Trang chủ có biểu đồ 6 tháng; Báo cáo có biểu đồ 12 tháng. Hai cột Thu/Chi dùng chung thang đo VNĐ, có chú giải, đỉnh thang đo và nhãn tháng/năm. Vuốt ngang để xem đủ các tháng; từng cột (kể cả cột 0) hỗ trợ chạm và trình đọc màn hình để xem giá trị chính xác cùng các giao dịch tương ứng.

Chạm ô thu, chi, số dư hoặc số dư chuyển tiếp để xem các khoản tạo nên số tiền đó. Chạm thẻ mục tiêu tiết kiệm để xem riêng lịch sử của mục tiêu. Không tạo dữ liệu minh họa trong bản cài đặt.

## Bố cục và giao dịch phiên bản 1.5

Trang chủ ưu tiên số dư, thu/chi và biểu đồ cột danh mục của tháng đang chọn. Biểu đồ sắp xếp theo số tiền giảm dần; chạm từng cột để xem tên danh mục, số tiền, tỷ lệ trên tổng chi của tháng và các giao dịch liên quan. Thu nhập và tháng khác không tính vào tỷ lệ. Biểu đồ dòng tiền 6 tháng nằm trong mục mở rộng bên dưới.

Các ô nhập tiền tự chia nhóm bằng dấu cách (`30000` → `30 000`); áp dụng cho giao dịch, lịch định kỳ, mục tiêu, góp/rút và ngân sách. Các số tiền hiển thị cũng dùng dấu cách. Dữ liệu SQLite vẫn lưu số nguyên VNĐ, không đổi đơn vị hoặc số tiền.

Trang Giao dịch nhóm theo ngày mới nhất trước, với tổng thu/chi của từng nhóm sau khi áp dụng bộ lọc. Bật Tất cả các tháng sẽ ẩn thanh chọn tháng; chọn một tháng vẫn giữ cách nhóm theo ngày.

Menu nổi lấy phần nội dung cuộn phía sau làm lớp blur riêng; icon và chữ ở trên giữ sắc nét. Nút lấy nền đã làm mờ và thêm lớp kính trong cùng hiệu ứng chạm. Menu và nút dùng bitmap nền thu nhỏ đã làm mờ trên mọi phiên bản Android, giảm chi phí vẽ và không làm mờ chữ.

## Giao diện và thu nhập phiên bản 1.6

Menu giữ nguyên khi đổi trang, giảm lớp trắng phủ để thấy nội dung cuộn qua phần kính mờ. Chỉ phần nền dưới menu bị blur; chữ và icon vẫn sắc nét. Bố cục Tài khoản gồm hồ sơ gọn, ngân sách tháng, danh mục thu/chi, lịch định kỳ, báo cáo và dữ liệu.

Danh mục thu nhập có thể thêm/đổi tên/xóa, dùng được trong form giao dịch, lịch định kỳ và bộ lọc `Thu · tên danh mục`. Chuyển loại Thu/Chi sẽ đổi danh sách và nhớ lựa chọn của từng loại trong lúc mở form. Xóa/đổi tên chỉ ảnh hưởng giao dịch đúng loại. SQLite nâng cấp lên phiên bản 5, giữ nguyên tiền, lịch và dữ liệu cũ.

Hiệu ứng Android native: chuyển trang fade/slide nhẹ, phản hồi nhấn nút, bảng chi tiết mở/đóng và cột biểu đồ tăng dần. Không dùng WebView hay JavaScript. Tôn trọng cài đặt tắt animation của hệ thống; animation được hủy khi view rời màn hình.

Kiểm chứng bản 1.6: build debug thành công, 3 kiểm thử JVM và 26 kiểm thử Android trên API 36.1 đều qua; lint không có lỗi (còn cảnh báo). Đã chụp kiểm tra Tài khoản, danh mục/form thu nhập và nội dung cuộn dưới menu. Chưa chạy trên điện thoại thật.

## Blur và avatar phiên bản 1.7

Menu và nền dialog lấy mẫu đúng tọa độ của giao diện phía sau, cập nhật ở mỗi khung hình có thay đổi. Vùng lấy mẫu có đệm và bỏ qua menu để tránh bóng chữ lặp lại; khi giao diện đứng yên không tự tạo vòng vẽ liên tục. Bảng thêm giao dịch cũng dùng nền kính mờ.

Chọn/thay/xóa ảnh đại diện bằng trình chọn ảnh hệ thống. Ảnh được thu nhỏ, xử lý hướng máy ảnh và lưu bản riêng trong ứng dụng; không xin quyền đọc toàn bộ thư viện. Tên và ảnh cùng hiển thị ở Trang chủ và Tài khoản.

## Summary phiên bản 1.8

Mở **Trang chủ → Summary · Tổng quan tháng** hoặc nút cùng tên trong Tài khoản. Trang dùng nền tối, biểu đồ donut có các đoạn màu bo nhẹ, tổng tiền ở giữa và danh sách danh mục theo số tiền giảm dần. Mỗi dòng có số giao dịch, số tiền và tỷ lệ trên tổng của tháng/loại đang chọn.

Chọn Thu nhập/Chi tiêu, đổi tháng bằng mũi tên hoặc chạm tên tháng để chọn tháng/năm. So sánh với tổng cùng loại của toàn bộ tháng trước, kể cả chuyển năm; tháng trước bằng 0 sẽ hiển thị số tiền thay vì chia cho 0. Chạm vòng tròn hoặc dòng danh mục để xem đúng các giao dịch tạo nên phần đó. Trạng thái không có dữ liệu hiển thị vòng tròn trống và nút thêm giao dịch.

Summary không sửa dữ liệu và không tính tiền góp tiết kiệm thành chi tiêu. Các màn hình và menu hiện có được giữ lại. APK giao bao gồm cả blur/avatar của bản 1.7.
