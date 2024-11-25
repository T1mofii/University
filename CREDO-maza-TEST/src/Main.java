import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Record {
    int godzina;
    int minuta;
    int sekunda;
    int milisekunda;
    String detektory;

    public Record(int godzina, int minuta, int sekunda, int milisekunda, String detektory) {
        this.godzina = godzina;
        this.minuta = minuta;
        this.sekunda = sekunda;
        this.milisekunda = milisekunda;
        this.detektory = detektory;
    }

    public long toMilliseconds() {
        return (godzina * 3600 + minuta * 60 + sekunda) * 1000L + milisekunda;
    }

    @Override
    public String toString() {
        return String.format("Godzina: %02d:%02d:%02d.%03d, Detektory: %s", godzina, minuta, sekunda, milisekunda, detektory);
    }
}

class TimeDifference {
    String detektory;
    Record firstRecord;
    Record secondRecord;
    long differenceMilliseconds;

    public TimeDifference(String detektory, Record firstRecord, Record secondRecord, long differenceMilliseconds) {
        this.detektory = detektory;
        this.firstRecord = firstRecord;
        this.secondRecord = secondRecord;
        this.differenceMilliseconds = differenceMilliseconds;
    }

    public String getDifferenceAsString() {
        long hours = differenceMilliseconds / (3600 * 1000);
        long minutes = (differenceMilliseconds % (3600 * 1000)) / (60 * 1000);
        long seconds = (differenceMilliseconds % (60 * 1000)) / 1000;
        long milliseconds = differenceMilliseconds % 1000;

        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);
    }
}

public class Main {
    public static void main(String[] args) {
        JFrame frame = new JFrame("CREDO-maza");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        JButton selectFileButton = new JButton("Wybierz plik CSV");

        JPanel tablePanel = new JPanel(new GridLayout(0, 1));
        JScrollPane tableScrollPane = new JScrollPane(tablePanel);
        tableScrollPane.setPreferredSize(new Dimension(780, 500));

        selectFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(frame);

                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    String filePath = selectedFile.getAbsolutePath();

                    List<Record> records = new ArrayList<>();
                    List<String> validDetectors = Arrays.asList("1---", "12--", "123-", "1234", "1--4");

                    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            String[] parts = line.split(",");
                            if (parts.length == 4) {
                                int godzina = Integer.parseInt(parts[0]);
                                int minuta = Integer.parseInt(parts[1]);
                                int sekunda = Integer.parseInt(parts[2].split("\\.")[0]);
                                int milisekunda = Integer.parseInt(parts[2].split("\\.")[1]);
                                String detektory = parts[3];

                                Record record = new Record(godzina, minuta, sekunda, milisekunda, detektory);
                                if (validDetectors.contains(detektory)) {
                                    records.add(record);
                                }
                            }
                        }
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(frame, "Błąd odczytu pliku: " + ex.getMessage());
                        return;
                    }

                    tablePanel.removeAll(); // Очистка таблицы перед загрузкой новых данных

                    for (String detector : validDetectors) {
                        List<Record> filteredRecords = new ArrayList<>();
                        List<TimeDifference> timeDifferences = new ArrayList<>();

                        for (Record record : records) {
                            if (record.detektory.equals(detector)) {
                                filteredRecords.add(record);
                            }
                        }

                        for (int i = 1; i < filteredRecords.size(); i++) {
                            Record prevRecord = filteredRecords.get(i - 1);
                            Record currRecord = filteredRecords.get(i);

                            long timeDifference = currRecord.toMilliseconds() - prevRecord.toMilliseconds();
                            TimeDifference diff = new TimeDifference(detector, prevRecord, currRecord, timeDifference);
                            timeDifferences.add(diff);
                        }

                        // Создание таблицы для текущего детектора
                        DefaultTableModel model = new DefaultTableModel();
                        model.addColumn("Pierwszy rekord");
                        model.addColumn("Drugi rekord");
                        model.addColumn("Różnica czasu");

                        for (TimeDifference diff : timeDifferences) {
                            model.addRow(new Object[]{
                                    diff.firstRecord.toString(),
                                    diff.secondRecord.toString(),
                                    diff.getDifferenceAsString()
                            });
                        }

                        JTable table = new JTable(model);
                        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
                        table.setFillsViewportHeight(true);

                        JLabel detectorLabel = new JLabel("Detektor: " + detector + " (Rekordów: " + filteredRecords.size() + ")");
                        detectorLabel.setFont(new Font("Arial", Font.BOLD, 14));

                        JPanel detectorPanel = new JPanel(new BorderLayout());
                        detectorPanel.add(detectorLabel, BorderLayout.NORTH);
                        detectorPanel.add(new JScrollPane(table), BorderLayout.CENTER);

                        tablePanel.add(detectorPanel);
                    }

                    tablePanel.revalidate();
                    tablePanel.repaint();
                }
            }
        });

        frame.add(selectFileButton, BorderLayout.NORTH);
        frame.add(tableScrollPane, BorderLayout.CENTER);

        frame.setVisible(true);
    }
}
