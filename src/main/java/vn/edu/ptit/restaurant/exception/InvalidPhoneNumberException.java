package vn.edu.ptit.restaurant.exception;

public final class InvalidPhoneNumberException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidPhoneNumberException() {
        super("Số điện thoại chưa đúng định dạng. Hãy nhập 10–11 chữ số bắt đầu bằng 0 hoặc dạng +84 tương ứng.");
    }
}
