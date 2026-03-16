package main.java.storage;

public class Page {
        
        public static final int PAGE_SIZE = 4096;

        private int pageId;
        private byte[] data;

        public Page(int pageId) {
            this.pageId = pageId;
            this.data = new byte[PAGE_SIZE];
        }

        public int getPageId() {
            return pageId;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            if (data.length != PAGE_SIZE) {
                throw new IllegalArgumentException("Data must be exactly " + PAGE_SIZE + " bytes");
            }
            this.data = data;
        }
}