import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Główna klasa aplikacji Randomizer - aplikacja do losowego przydzielania par
 * z systemem ograniczeń i losowania liter w kolejnych turach
 */
public class RandomizerApp extends JFrame {
    // === KOMPONENTY INTERFEJSU UŻYTKOWNIKA ===
    
    // Pole tekstowe do wpisywania nazw uczestników
    private JTextArea nameListArea;
    // Panel wyświetlający załadowane nazwy jako kolorowe etykiety
    private JPanel nameDisplayPanel;
    // Listy rozwijane do wyboru osób dla ograniczeń
    private JComboBox<String> person1Select, person2Select;
    // Model i lista do wyświetlania ograniczeń par
    private DefaultListModel<String> restrictionsModel;
    private JList<String> restrictionsList;
    // Etykiety pokazujące status gry
    private JLabel gameStatusLabel, currentTurnLabel;
    // Przyciski głównych funkcji aplikacji
    private JButton loadNamesBtn, addRestrictionBtn, beginRollingBtn, downloadResultsBtn;
    // Panel wyświetlający finalne wyniki losowania
    private JPanel pairResultsPanel;
    
    // === DANE APLIKACJI ===
    
    // Lista wszystkich uczestników gry
    private List<String> names = new ArrayList<>();
    // Lista ograniczeń - pary które nie mogą być ze sobą dopasowane
    private List<Restriction> restrictions = new ArrayList<>();
    // Indeks aktualnie losującego gracza
    private int currentTurnIndex = 0;
    // Lista wyników każdego gracza (imię + wylosowana litera)
    private List<PlayerResult> playerResults = new ArrayList<>();
    // Flaga informująca czy gra jest w toku
    private boolean gameInProgress = false;
    // Lista finalnych przydziałów (kto kogo wylosował z jaką literą)
    private List<Assignment> finalPairs = new ArrayList<>();
    
    /**
     * Konstruktor - inicjalizuje główne okno aplikacji
     */
    public RandomizerApp() {
        // Ustawienia okna
        setTitle("Pair Randomizer - Java Version");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Inicjalizacja komponentów i układu
        initializeComponents();
        setupLayout();
        setupEventListeners();
        
        // Finalne ustawienia okna - zwiększone rozmiary
        pack();
        setLocationRelativeTo(null); // Wyśrodkowanie okna
        setMinimumSize(new Dimension(1200, 900)); // Zwiększony minimalny rozmiar
        setPreferredSize(new Dimension(1400, 1000)); // Preferowany rozmiar
        setSize(1400, 1000); // Ustawienie początkowego rozmiaru
    }
    
    /**
     * Inicjalizuje wszystkie komponenty interfejsu użytkownika
     */
    private void initializeComponents() {
        // === SEKCJA NAZW ===
        // Pole tekstowe do wprowadzania nazw uczestników
        nameListArea = new JTextArea(5, 40);
        nameListArea.setBorder(BorderFactory.createTitledBorder("Wprowadź nazwy oddzielone przecinkami lub w nowych liniach"));
        
        // Panel do wyświetlania załadowanych nazw
        nameDisplayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // Przycisk do załadowania nazw z pola tekstowego
        loadNamesBtn = new JButton("Załaduj Nazwy");
        
        // === SEKCJA OGRANICZEŃ ===
        // Listy rozwijane do wyboru osób dla ograniczeń
        person1Select = new JComboBox<>();
        person2Select = new JComboBox<>();
        
        // Przycisk do dodania ograniczenia
        addRestrictionBtn = new JButton("Dodaj Ograniczenie");
        
        // Model i lista do wyświetlania aktualnych ograniczeń
        restrictionsModel = new DefaultListModel<>();
        restrictionsList = new JList<>(restrictionsModel);
        restrictionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // === SEKCJA STATUS GRY ===
        // Etykiety informujące o stanie gry
        gameStatusLabel = new JLabel("Załaduj nazwy aby rozpocząć grę");
        currentTurnLabel = new JLabel("");
        
        // Przycisk rozpoczynający losowanie
        beginRollingBtn = new JButton("Rozpocznij Losowanie");
        beginRollingBtn.setEnabled(false); // Początkowo nieaktywny
        
        // === SEKCJA WYNIKÓW ===
        // Panel do wyświetlania finalnych wyników
        pairResultsPanel = new JPanel();
        pairResultsPanel.setLayout(new BoxLayout(pairResultsPanel, BoxLayout.Y_AXIS));
        
        // Przycisk do pobierania wyników do pliku
        downloadResultsBtn = new JButton("Pobierz Wyniki (TXT)");
        downloadResultsBtn.setEnabled(false); // Aktywny dopiero po zakończeniu gry
    }
    
    /**
     * Tworzy układ interfejsu użytkownika z zakładkami
     */
    private void setupLayout() {
        // Główny panel z zakładkami
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // === ZAKŁADKA "NAZWY" ===
        JPanel namesPanel = new JPanel(new BorderLayout());
        namesPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Górna część - pole tekstowe i przycisk
        JPanel namesTopPanel = new JPanel(new BorderLayout());
        namesTopPanel.add(new JScrollPane(nameListArea), BorderLayout.CENTER);
        namesTopPanel.add(loadNamesBtn, BorderLayout.SOUTH);
        
        // Składanie sekcji nazw
        namesPanel.add(namesTopPanel, BorderLayout.NORTH);
        namesPanel.add(new JScrollPane(nameDisplayPanel), BorderLayout.CENTER);
        
        tabbedPane.addTab("Nazwy", namesPanel);
        
        // === ZAKŁADKA "OGRANICZENIA" ===
        JPanel restrictionsPanel = new JPanel(new BorderLayout());
        restrictionsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Panel kontrolny do dodawania ograniczeń
        JPanel restrictionsControlPanel = new JPanel(new FlowLayout());
        restrictionsControlPanel.add(new JLabel("Osoba 1:"));
        restrictionsControlPanel.add(person1Select);
        restrictionsControlPanel.add(new JLabel("nie może być sparowana z"));
        restrictionsControlPanel.add(person2Select);
        restrictionsControlPanel.add(addRestrictionBtn);
        
        // Składanie sekcji ograniczeń
        restrictionsPanel.add(new JLabel("Dodaj pary, które NIE powinny być dopasowane razem:"), BorderLayout.NORTH);
        restrictionsPanel.add(restrictionsControlPanel, BorderLayout.CENTER);
        restrictionsPanel.add(new JScrollPane(restrictionsList), BorderLayout.SOUTH);
        
        tabbedPane.addTab("Ograniczenia", restrictionsPanel);
        
        // === ZAKŁADKA "LOSOWANIE" ===
        JPanel rollingPanel = new JPanel(new BorderLayout());
        rollingPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Panel statusu gry
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(gameStatusLabel, BorderLayout.NORTH);
        statusPanel.add(currentTurnLabel, BorderLayout.CENTER);
        statusPanel.add(beginRollingBtn, BorderLayout.SOUTH);
        
        rollingPanel.add(statusPanel, BorderLayout.NORTH);
        
        tabbedPane.addTab("Losowanie", rollingPanel);
        
        // === ZAKŁADKA "WYNIKI" ===
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        resultsPanel.add(new JScrollPane(pairResultsPanel), BorderLayout.CENTER);
        resultsPanel.add(downloadResultsBtn, BorderLayout.SOUTH);
        
        tabbedPane.addTab("Wyniki", resultsPanel);
        
        // Dodanie głównego panelu do okna
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    /**
     * Ustawia nasłuchiwanie zdarzeń dla przycisków i innych komponentów
     */
    private void setupEventListeners() {
        // Przycisk załadowania nazw
        loadNamesBtn.addActionListener(e -> loadNames());
        
        // Przycisk dodania ograniczenia
        addRestrictionBtn.addActionListener(e -> addRestriction());
        
        // Przycisk rozpoczęcia losowania
        beginRollingBtn.addActionListener(e -> startRolling());
        
        // Przycisk pobierania wyników
        downloadResultsBtn.addActionListener(e -> downloadResults());
        
        // Dwukrotne kliknięcie na liście ograniczeń - usunięcie ograniczenia
        restrictionsList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int index = restrictionsList.locationToIndex(evt.getPoint());
                    if (index >= 0) {
                        restrictions.remove(index);
                        updateRestrictionsDisplay();
                    }
                }
            }
        });
    }
    
    /**
     * Ładuje nazwy uczestników z pola tekstowego i inicjalizuje grę
     */
    private void loadNames() {
        String text = nameListArea.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Proszę wprowadzić przynajmniej 2 nazwy.");
            return;
        }
        
        // Czyszczenie poprzednich danych
        names.clear();
        
        // Podział tekstu na nazwy (przecinki lub nowe linie)
        String[] splitNames = text.split("[,\\n]");
        for (String name : splitNames) {
            String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                names.add(trimmed);
            }
        }
        
        // Walidacja minimalnej liczby uczestników
        if (names.size() < 2) {
            JOptionPane.showMessageDialog(this, "Proszę wprowadzić przynajmniej 2 nazwy.");
            return;
        }
        
        // Reset i aktualizacja interfejsu
        restrictions.clear();
        updateNameDisplay();
        updateSelectOptions();
        updateRestrictionsDisplay();
        startGame();
    }
    
    /**
     * Aktualizuje wyświetlanie nazw jako kolorowe etykiety
     */
    private void updateNameDisplay() {
        nameDisplayPanel.removeAll();
        for (String name : names) {
            // Tworzenie kolorowej etykiety dla każdej nazwy
            JLabel nameLabel = new JLabel(name);
            nameLabel.setOpaque(true);
            nameLabel.setBackground(new Color(40, 167, 69)); // Zielone tło
            nameLabel.setForeground(Color.WHITE);
            nameLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            nameDisplayPanel.add(nameLabel);
        }
        // Odświeżenie wyświetlania
        nameDisplayPanel.revalidate();
        nameDisplayPanel.repaint();
    }
    
    /**
     * Aktualizuje opcje w listach rozwijanych dla ograniczeń
     */
    private void updateSelectOptions() {
        // Czyszczenie poprzednich opcji
        person1Select.removeAllItems();
        person2Select.removeAllItems();
        
        // Dodanie opcji domyślnych
        person1Select.addItem("Wybierz Osobę 1");
        person2Select.addItem("Wybierz Osobę 2");
        
        // Dodanie wszystkich nazw do list rozwijanych
        for (String name : names) {
            person1Select.addItem(name);
            person2Select.addItem(name);
        }
    }
    
    /**
     * Dodaje nowe ograniczenie pary (dwie osoby które nie mogą być razem)
     */
    private void addRestriction() {
        String person1 = (String) person1Select.getSelectedItem();
        String person2 = (String) person2Select.getSelectedItem();
        
        // Walidacja wyboru osób
        if (person1 == null || person2 == null ||
            person1.startsWith("Wybierz") || person2.startsWith("Wybierz")) {
            JOptionPane.showMessageDialog(this, "Proszę wybrać obie osoby dla ograniczenia.");
            return;
        }
        
        // Sprawdzenie czy osoba nie jest ograniczona sama ze sobą
        if (person1.equals(person2)) {
            JOptionPane.showMessageDialog(this, "Osoba nie może być ograniczona sama ze sobą.");
            return;
        }
        
        // Sprawdzenie czy ograniczenie już istnieje (w obie strony)
        boolean exists = restrictions.stream().anyMatch(r ->
            (r.person1.equals(person1) && r.person2.equals(person2)) ||
            (r.person1.equals(person2) && r.person2.equals(person1))
        );
        
        if (exists) {
            JOptionPane.showMessageDialog(this, "To ograniczenie już istnieje.");
            return;
        }
        
        // Dodanie nowego ograniczenia
        restrictions.add(new Restriction(person1, person2));
        updateRestrictionsDisplay();
        
        // Reset list rozwijanych
        person1Select.setSelectedIndex(0);
        person2Select.setSelectedIndex(0);
    }
    
    /**
     * Aktualizuje wyświetlanie listy ograniczeń
     */
    private void updateRestrictionsDisplay() {
        restrictionsModel.clear();
        for (Restriction r : restrictions) {
            restrictionsModel.addElement(r.person1 + " ↔ " + r.person2);
        }
    }
    
    /**
     * Rozpoczyna nową grę - resetuje wszystkie dane
     */
    private void startGame() {
        if (names.size() < 2) return;
        
        // Reset stanu gry
        gameInProgress = true;
        currentTurnIndex = 0;
        playerResults.clear();
        finalPairs.clear();
        
        // Aktywacja przycisku losowania
        beginRollingBtn.setEnabled(true);
        downloadResultsBtn.setEnabled(false);
        
        updateGameStatus();
    }
    
    /**
     * Aktualizuje wyświetlanie statusu gry
     */
    private void updateGameStatus() {
        if (currentTurnIndex < names.size()) {
            // Jeszcze nie wszyscy wylosowali
            gameStatusLabel.setText("Gotowy do rozpoczęcia losowania - " + names.size() + " graczy łącznie");
            currentTurnLabel.setText("Kliknij 'Rozpocznij Losowanie' aby rozpocząć losowanie kolejnych tur");
        } else {
            // Wszyscy wylosowali - czas na generowanie par
            gameStatusLabel.setText("Wszyscy gracze wylosowali! Generowanie par...");
            currentTurnLabel.setText("");
            generatePairsAutomatically();
        }
    }
    
    /**
     * Rozpoczyna proces losowania - wyłącza przycisk i pokazuje okno modalne
     */
    private void startRolling() {
        beginRollingBtn.setEnabled(false);
        showRollingModal();
    }
    
    /**
     * Pokazuje okno modalne dla aktualnego gracza do losowania litery
     */
    private void showRollingModal() {
        if (currentTurnIndex >= names.size()) {
            updateGameStatus();
            return;
        }
        
        // Pobranie aktualnego gracza
        String currentPlayer = names.get(currentTurnIndex);
        
        // Utworzenie i wyświetlenie okna modalnego
        RollingDialog dialog = new RollingDialog(this, currentPlayer, currentTurnIndex + 1, names.size());
        dialog.setVisible(true);
        
        // Przetworzenie wyniku losowania
        if (dialog.getResult() != null) {
            playerResults.add(dialog.getResult());
            currentTurnIndex++;
            
            // Przejście do następnego gracza lub zakończenie
            SwingUtilities.invokeLater(() -> {
                if (currentTurnIndex < names.size()) {
                    showRollingModal(); // Następny gracz
                } else {
                    updateGameStatus(); // Wszyscy skończyli
                }
            });
        }
    }
    
    /**
     * Generuje losową literę, unikając już użytych liter
     * @return losowa litera z alfabetu
     */
    private char getRandomLetter() {
        String allLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Set<Character> usedLetters = new HashSet<>();
        
        // Zbieranie już użytych liter
        for (PlayerResult result : playerResults) {
            usedLetters.add(result.letter);
        }
        
        // Tworzenie listy dostępnych liter
        List<Character> availableLetters = new ArrayList<>();
        for (char c : allLetters.toCharArray()) {
            if (!usedLetters.contains(c)) {
                availableLetters.add(c);
            }
        }
        
        // Zwracanie losowej dostępnej litery lub dowolnej jeśli wszystkie użyte
        if (availableLetters.isEmpty()) {
            return allLetters.charAt(new Random().nextInt(allLetters.length()));
        }
        
        return availableLetters.get(new Random().nextInt(availableLetters.size()));
    }
    
    /**
     * Sprawdza czy dana para jest ograniczona (nie może być razem)
     * @param name1 pierwsza osoba
     * @param name2 druga osoba
     * @return true jeśli para jest ograniczona
     */
    private boolean isRestrictedPair(String name1, String name2) {
        return restrictions.stream().anyMatch(r ->
            (r.person1.equals(name1) && r.person2.equals(name2)) ||
            (r.person1.equals(name2) && r.person2.equals(name1))
        );
    }
    
    /**
     * Automatycznie generuje finalne przydziały par na podstawie wylosowanych liter
     * Każda osoba musi kogoś wylosować i zostać wylosowana
     */
    private void generatePairsAutomatically() {
        if (playerResults.size() != names.size()) return;
        
        List<Assignment> assignments = new ArrayList<>();
        List<String> availableTargets = new ArrayList<>(names); // Kto może zostać wylosowany
        List<String> peopleWhoNeedToAssign = new ArrayList<>(names); // Kto musi kogoś wylosować
        
        // Mieszanie list dla losowości
        Collections.shuffle(availableTargets);
        Collections.shuffle(peopleWhoNeedToAssign);
        
        // Przydzielanie każdej osobie celu
        for (String drawer : peopleWhoNeedToAssign) {
            boolean assigned = false;
            
            // Szukanie prawidłowego celu (nie siebie + nie ograniczonego)
            for (int i = 0; i < availableTargets.size(); i++) {
                String target = availableTargets.get(i);
                
                if (!target.equals(drawer) && !isRestrictedPair(drawer, target)) {
                    // Znalezienie wyniku losowania dla tej osoby
                    PlayerResult drawerResult = playerResults.stream()
                        .filter(r -> r.name.equals(drawer))
                        .findFirst().orElse(null);
                    
                    if (drawerResult != null) {
                        // Utworzenie przydziału
                        assignments.add(new Assignment(drawer, target, drawerResult.letter));
                        availableTargets.remove(i);
                        assigned = true;
                        break;
                    }
                }
            }
            
            // Jeśli nie znaleziono prawidłowego, przydziel pierwszy dostępny
            if (!assigned && !availableTargets.isEmpty()) {
                String target = availableTargets.stream()
                    .filter(t -> !t.equals(drawer))
                    .findFirst()
                    .orElse(availableTargets.get(0));
                
                PlayerResult drawerResult = playerResults.stream()
                    .filter(r -> r.name.equals(drawer))
                    .findFirst().orElse(null);
                
                if (drawerResult != null) {
                    assignments.add(new Assignment(drawer, target, drawerResult.letter));
                    availableTargets.remove(target);
                }
            }
        }
        
        // Zapisanie wyników i wyświetlenie
        finalPairs = assignments;
        displayAssignments();
        downloadResultsBtn.setEnabled(true);
    }
    
    /**
     * Wyświetla finalne przydziały w panelu wyników
     */
    private void displayAssignments() {
        pairResultsPanel.removeAll();
        
        // Tytuł sekcji wyników
        JLabel titleLabel = new JLabel("Wylosowane Pary (Każda osoba losuje kogoś):");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        pairResultsPanel.add(titleLabel);
        
        if (finalPairs.isEmpty()) {
            // Brak przydziałów - błąd
            pairResultsPanel.add(new JLabel("Nie udało się wygenerować przydziałów z obecnymi ograniczeniami."));
        } else {
            // Wyświetlanie każdego przydziału
            for (Assignment assignment : finalPairs) {
                JPanel assignmentPanel = new JPanel(new BorderLayout());
                assignmentPanel.setBorder(BorderFactory.createEtchedBorder());
                assignmentPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                
                // Nazwy osób (kto → kogo)
                JLabel namesLabel = new JLabel(assignment.drawer + " → " + assignment.target);
                namesLabel.setFont(namesLabel.getFont().deriveFont(Font.BOLD));
                
                // Wylosowana litera
                JLabel letterLabel = new JLabel(String.valueOf(assignment.letter));
                letterLabel.setFont(letterLabel.getFont().deriveFont(Font.BOLD, 20f));
                letterLabel.setForeground(new Color(255, 107, 107));
                letterLabel.setHorizontalAlignment(SwingConstants.CENTER);
                letterLabel.setPreferredSize(new Dimension(40, 30));
                
                assignmentPanel.add(namesLabel, BorderLayout.CENTER);
                assignmentPanel.add(letterLabel, BorderLayout.EAST);
                
                pairResultsPanel.add(assignmentPanel);
            }
        }
        
        // Odświeżenie wyświetlania
        pairResultsPanel.revalidate();
        pairResultsPanel.repaint();
    }
    
    /**
     * Zapisuje wyniki do pliku tekstowego
     */
    private void downloadResults() {
        if (finalPairs.isEmpty()) return;
        
        // Okno dialogowe wyboru pliku
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new java.io.File("randomizer-wyniki-" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".txt"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (FileWriter writer = new FileWriter(fileChooser.getSelectedFile())) {
                // Nagłówek pliku
                writer.write("RANDOMIZER - JAVA VERSION\n");
                writer.write("=================================\n\n");
                writer.write("Wygenerowano: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
                writer.write("Liczba uczestników: " + names.size() + "\n\n");
                
                // Zapisanie każdego przydziału
                for (Assignment assignment : finalPairs) {
                    writer.write(assignment.drawer + " → " + assignment.target + " → " + assignment.letter + "\n");
                }
                
                JOptionPane.showMessageDialog(this, "Wyniki zostały zapisane do pliku!");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Błąd podczas zapisywania pliku: " + e.getMessage());
            }
        }
    }
    
    /**
     * Metoda publiczna do uzyskania losowej litery (używana przez okno modalne)
     */
    public char getRandomLetterForModal() {
        return getRandomLetter();
    }
    
    /**
     * Metoda publiczna do uzyskania listy nazw (używana przez okno modalne)
     */
    public List<String> getNames() {
        return new ArrayList<>(names);
    }
    
    /**
     * Główna metoda - punkt wejścia aplikacji
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Ustawienie systemowego wyglądu i zachowania
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // Utworzenie i wyświetlenie głównego okna
            new RandomizerApp().setVisible(true);
        });
    }
    
    // === KLASY WEWNĘTRZNE DO PRZECHOWYWANIA DANYCH ===
    
    /**
     * Klasa reprezentująca ograniczenie - parę osób które nie mogą być razem
     */
    static class Restriction {
        String person1, person2; // Dwie osoby w ograniczeniu
        
        Restriction(String person1, String person2) {
            this.person1 = person1;
            this.person2 = person2;
        }
    }
    
    /**
     * Klasa reprezentująca wynik losowania jednego gracza
     */
    static class PlayerResult {
        String name;  // Imię gracza
        char letter;  // Wylosowana litera
        
        PlayerResult(String name, char letter) {
            this.name = name;
            this.letter = letter;
        }
    }
    
    /**
     * Klasa reprezentująca finalny przydział - kto kogo wylosował z jaką literą
     */
    static class Assignment {
        String drawer;  // Kto losuje
        String target;  // Kogo wylosował
        char letter;    // Z jaką literą
        
        Assignment(String drawer, String target, char letter) {
            this.drawer = drawer;
            this.target = target;
            this.letter = letter;
        }
    }
}