/**
 * Sistem Antrian Bank dengan Anti-Starvation (antrian ganda + rasio).
 * Struktur data: Circular Array Queue buatan sendiri (tanpa Java Collections Framework).
 *
 * Aturan: VIP dilayani lebih dulu, tetapi setelah RASIO (default 3) VIP berturut-turut
 * dilayani sementara ada nasabah reguler yang menunggu, 1 nasabah reguler wajib dilayani.
 */
public class BankQueueSimulation {

    // ---------- Entitas nasabah ----------
    static class Nasabah {
        final int id;
        final String nama;
        final boolean vip;

        Nasabah(int id, String nama, boolean vip) {
            this.id = id;
            this.nama = nama;
            this.vip = vip;
        }

        @Override
        public String toString() {
            return (vip ? "VIP-" : "REG-") + id + "(" + nama + ")";
        }
    }

    // ---------- Circular Array Queue buatan sendiri ----------
    static class CircularQueue {
        private Nasabah[] data;
        private int front;  // indeks elemen terdepan
        private int size;   // jumlah elemen

        CircularQueue(int kapasitasAwal) {
            data = new Nasabah[kapasitasAwal];
            front = 0;
            size = 0;
        }

        boolean isEmpty() { return size == 0; }
        int size() { return size; }

        // O(1) amortized: O(n) hanya saat array penuh dan digandakan
        void enqueue(Nasabah n) {
            if (size == data.length) resize();
            int rear = (front + size) % data.length;
            data[rear] = n;
            size++;
        }

        // O(1)
        Nasabah dequeue() {
            if (isEmpty()) return null;
            Nasabah n = data[front];
            data[front] = null;                 // bantu garbage collector
            front = (front + 1) % data.length;  // maju melingkar
            size--;
            return n;
        }

        Nasabah peek() { return isEmpty() ? null : data[front]; }

        // Menggandakan kapasitas; elemen disalin berurutan mulai indeks 0 ("meluruskan" lingkaran)
        private void resize() {
            Nasabah[] baru = new Nasabah[data.length * 2];
            for (int i = 0; i < size; i++) {
                baru[i] = data[(front + i) % data.length];
            }
            data = baru;
            front = 0;
        }

        String tampil() {
            if (isEmpty()) return "[kosong]";
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < size; i++) {
                sb.append(data[(front + i) % data.length]);
                if (i < size - 1) sb.append(", ");
            }
            return sb.append("]").toString();
        }
    }

    // ---------- Manajer antrian bank ----------
    static class AntrianBank {
        private final CircularQueue antrianVIP = new CircularQueue(4);
        private final CircularQueue antrianReguler = new CircularQueue(4);
        private final int rasio;          // maksimum VIP berturut-turut jika reguler menunggu
        private int vipBerturut = 0;      // penghitung VIP berturut-turut
        private int idVip = 0, idReg = 0;

        AntrianBank(int rasio) { this.rasio = rasio; }

        void enqueueVIP(String nama) {
            antrianVIP.enqueue(new Nasabah(++idVip, nama, true));
        }

        void enqueueReguler(String nama) {
            antrianReguler.enqueue(new Nasabah(++idReg, nama, false));
        }

        /** Memilih nasabah berikutnya sesuai aturan anti-starvation. */
        Nasabah dequeue() {
            if (antrianVIP.isEmpty() && antrianReguler.isEmpty()) return null;

            boolean layaniReguler;
            if (antrianVIP.isEmpty()) {
                layaniReguler = true;                              // tidak ada VIP
            } else if (antrianReguler.isEmpty()) {
                layaniReguler = false;                             // tidak ada reguler
            } else {
                layaniReguler = (vipBerturut >= rasio);            // jatah reguler tiba
            }

            Nasabah dipanggil;
            if (layaniReguler) {
                dipanggil = antrianReguler.dequeue();
                vipBerturut = 0;                                   // reset penghitung
            } else {
                dipanggil = antrianVIP.dequeue();
                vipBerturut++;
            }
            return dipanggil;
        }

        void displayQueue() {
            System.out.println("   VIP     : " + antrianVIP.tampil());
            System.out.println("   Reguler : " + antrianReguler.tampil());
        }

        boolean kosong() { return antrianVIP.isEmpty() && antrianReguler.isEmpty(); }
        int jumlahReguler() { return antrianReguler.size(); }
    }

    // ---------- Simulasi ----------
    static void panggilSemua(AntrianBank bank, String label) {
        System.out.println("\n>> Urutan pemanggilan (" + label + "):");
        int nomor = 1;
        int vipBerturut = 0, maksVipBerturut = 0;
        StringBuilder posisiReg = new StringBuilder();

        while (!bank.kosong()) {
            int regMenunggu = bank.jumlahReguler();
            Nasabah n = bank.dequeue();
            System.out.printf("   %2d. %s%n", nomor, n);

            if (n.vip && regMenunggu > 0) {          // VIP dilayani padahal ada reguler menunggu
                vipBerturut++;
                if (vipBerturut > maksVipBerturut) maksVipBerturut = vipBerturut;
            } else {
                vipBerturut = 0;
            }
            if (!n.vip) posisiReg.append(posisiReg.length() == 0 ? "" : ", ").append(nomor);
            nomor++;
        }
        System.out.println("   --> Maks. VIP berturut-turut saat reguler menunggu : " + maksVipBerturut);
        System.out.println("   --> Reguler dipanggil pada urutan ke-              : " + (posisiReg.length() == 0 ? "-" : posisiReg));
    }

    public static void main(String[] args) {
        final int RASIO = 3;
        System.out.println("=== SIMULASI ANTRIAN BANK (anti-starvation, rasio " + RASIO + " VIP : 1 Reguler) ===");

        // Skenario 1: 4 reguler datang lebih dulu, lalu 12 VIP datang beruntun (menguji starvation)
        System.out.println("\n--- Skenario 1: reguler datang dahulu, lalu banjir VIP ---");
        AntrianBank bank1 = new AntrianBank(RASIO);
        String[] reg = {"Andi", "Budi", "Citra", "Dewi"};
        for (String r : reg) { bank1.enqueueReguler(r); }
        for (int i = 1; i <= 12; i++) { bank1.enqueueVIP("V" + i); }
        System.out.println("Kondisi antrian awal:");
        bank1.displayQueue();
        panggilSemua(bank1, "Skenario 1");

        // Skenario 2: kedatangan dan pelayanan berselang-seling (arus VIP terus-menerus)
        System.out.println("\n--- Skenario 2: kedatangan & pelayanan berselang-seling ---");
        AntrianBank bank2 = new AntrianBank(RASIO);
        bank2.enqueueReguler("Eko");
        bank2.enqueueReguler("Fitri");
        int vipKe = 1;
        for (int putaran = 1; putaran <= 8; putaran++) {
            bank2.enqueueVIP("V" + vipKe++);
            bank2.enqueueVIP("V" + vipKe++);   // 2 VIP datang, 1 dilayani -> VIP menumpuk terus
            Nasabah n = bank2.dequeue();
            System.out.printf("Putaran %d: dipanggil %-14s | sisa reguler: %d%n", putaran, n, bank2.jumlahReguler());
        }
        System.out.println("Kondisi antrian setelah 8 putaran:");
        bank2.displayQueue();
        panggilSemua(bank2, "sisa antrian Skenario 2");

        // Skenario 3: kasus tepi
        System.out.println("\n--- Skenario 3: kasus tepi (hanya reguler / antrian kosong) ---");
        AntrianBank bank3 = new AntrianBank(RASIO);
        System.out.println("dequeue() pada antrian kosong -> " + bank3.dequeue());
        bank3.enqueueReguler("Gita");
        bank3.enqueueReguler("Hadi");
        panggilSemua(bank3, "hanya reguler");
    }
}
