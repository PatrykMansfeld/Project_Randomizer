import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

/**
 * Klasa RollingDialog - okno modalne do losowania liter przez poszczególnych graczy
 * Wyświetla się dla każdego gracza w jego turze i pozwala na wylosowanie litery
 */
public class RollingDialog extends JDialog {
    // === DANE GRACZA I TURY ===
    
    // Imię aktualnie losującego gracza
    private String playerName;
    // Numer aktualnej tury (1, 2, 3...)
    private int turnNumber;
    // Całkowita liczba graczy w grze
    private int totalPlayers;
    // Wynik losowania tego gracza (imię + litera)
    private RandomizerApp.PlayerResult result;
    
    // === KOMPONENTY INTERFEJSU ===
    
    // Etykieta z imieniem gracza i informacją o turze
    private JLabel playerLabel;
    // Etykieta pokazująca postęp (np. "Tura 2 z 5")
    private JLabel turnInfoLabel;
    // Etykieta z instrukcjami dla gracza
    private JLabel instructionsLabel;
    // Przycisk do losowania litery
    private JButton rollButton;
    // Panel wyświetlający wynik losowania
    private JPanel resultPanel;
    // Przycisk przechodzący do następnego gracza
    private JButton nextButton;
    
    /**
     * Konstruktor okna modalnego dla losowania litery
     * @param parent główne okno aplikacji
     * @param playerName imię aktualnie losującego gracza
     * @param turnNumber numer tury (1-based)
     * @param totalPlayers całkowita liczba graczy
     */
    public RollingDialog(RandomizerApp parent, String playerName, int turnNumber, int totalPlayers) {
        // Utworzenie okna modalnego (blokuje główne okno)
        super(parent, "Losowanie Litery", true);
        
        // Zapisanie parametrów
        this.playerName = playerName;
        this.turnNumber = turnNumber;
        this.totalPlayers = totalPlayers;
        
        // Inicjalizacja komponentów i układu
        initializeComponents();
        setupLayout();
        setupEventListeners();
        
        // Finalne ustawienia okna - zwiększone rozmiary
        pack(); // Dopasowanie rozmiaru do zawartości
        setSize(700, 600); // Zwiększony rozmiar okna modalnego
        setMinimumSize(new Dimension(650, 550)); // Minimalny rozmiar
        setLocationRelativeTo(parent); // Wyśrodkowanie względem głównego okna
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // Nie pozwalaj na zamknięcie bez kliknięcia przycisku
    }
    
    /**
     * Inicjalizuje wszystkie komponenty okna modalnego
     */
    private void initializeComponents() {
        // === NAGŁÓWEK Z IMIENIEM GRACZA ===
        playerLabel = new JLabel(playerName + " - Twoja Kolej");
        playerLabel.setFont(playerLabel.getFont().deriveFont(Font.BOLD, 24f)); // Duża, pogrubiona czcionka
        playerLabel.setHorizontalAlignment(SwingConstants.CENTER); // Wyśrodkowanie tekstu
        
        // === INFORMACJA O POSTĘPIE TURY ===
        turnInfoLabel = new JLabel("Tura " + turnNumber + " z " + totalPlayers);
        turnInfoLabel.setFont(turnInfoLabel.getFont().deriveFont(Font.PLAIN, 16f)); // Średnia czcionka
        turnInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        turnInfoLabel.setForeground(Color.GRAY); // Szary kolor dla mniej ważnej informacji
        
        // === INSTRUKCJE DLA GRACZA ===
        instructionsLabel = new JLabel(playerName + ", kliknij 'Losuj Literę' aby otrzymać swoją literę");
        instructionsLabel.setFont(instructionsLabel.getFont().deriveFont(Font.BOLD, 14f));
        instructionsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        // Stylowanie jako kolorowy panel z obramowaniem
        instructionsLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(33, 150, 243), 2), // Niebieskie obramowanie
            BorderFactory.createEmptyBorder(15, 20, 15, 20) // Wewnętrzne odstępy
        ));
        instructionsLabel.setBackground(new Color(227, 242, 253)); // Jasnoniebiskie tło
        instructionsLabel.setOpaque(true); // Włączenie wyświetlania tła
        
        // === PRZYCISK LOSOWANIA ===
        rollButton = new JButton("Losuj Literę");
        rollButton.setFont(rollButton.getFont().deriveFont(Font.BOLD, 18f)); // Duża czcionka
        rollButton.setPreferredSize(new Dimension(200, 60)); // Duży przycisk
        rollButton.setBackground(new Color(255, 107, 107)); // Czerwone tło
        rollButton.setForeground(Color.WHITE); // Biały tekst
        rollButton.setFocusPainted(false); // Wyłączenie ramki focus
        rollButton.setBorderPainted(false); // Wyłączenie standardowego obramowania
        
        // === PANEL WYNIKÓW (początkowo ukryty) ===
        resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS)); // Układanie pionowe
        resultPanel.setPreferredSize(new Dimension(400, 120));
        // Stylowanie jako kolorowy panel z obramowaniem
        resultPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 107, 107), 3), // Czerwone obramowanie
            BorderFactory.createEmptyBorder(20, 20, 20, 20) // Wewnętrzne odstępy
        ));
        resultPanel.setBackground(new Color(255, 224, 224)); // Jasnoczerwonawe tło
        resultPanel.setVisible(false); // Początkowo ukryty
        
        // === PRZYCISK NASTĘPNEGO GRACZA ===
        nextButton = new JButton("Następny Gracz");
        nextButton.setFont(nextButton.getFont().deriveFont(Font.BOLD, 16f));
        nextButton.setPreferredSize(new Dimension(180, 40));
        nextButton.setBackground(new Color(81, 207, 102)); // Zielone tło
        nextButton.setForeground(Color.WHITE); // Biały tekst
        nextButton.setFocusPainted(false);
        nextButton.setBorderPainted(false);
        nextButton.setEnabled(false); // Początkowo nieaktywny
    }
    
    /**
     * Tworzy układ komponentów w oknie modalnym
     */
    private void setupLayout() {
        setLayout(new BorderLayout()); // Główny układ BorderLayout
        
        // === PANEL NAGŁÓWKA ===
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20)); // Odstępy
        headerPanel.add(playerLabel, BorderLayout.CENTER); // Imię gracza na środku
        headerPanel.add(turnInfoLabel, BorderLayout.SOUTH); // Info o turze na dole
        
        // === PANEL INSTRUKCJI ===
        JPanel instructionsPanel = new JPanel(new BorderLayout());
        instructionsPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        instructionsPanel.add(instructionsLabel, BorderLayout.CENTER);
        
        // === PANEL PRZYCISKU LOSOWANIA ===
        JPanel buttonPanel = new JPanel(new FlowLayout()); // Wyśrodkowanie przycisku
        buttonPanel.add(rollButton);
        
        // === KONTENER PANELU WYNIKÓW ===
        JPanel resultContainer = new JPanel(new BorderLayout());
        resultContainer.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        resultContainer.add(resultPanel, BorderLayout.CENTER);
        
        // === PANEL PRZYCISKU NASTĘPNEGO GRACZA ===
        JPanel nextPanel = new JPanel(new FlowLayout());
        nextPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        nextPanel.add(nextButton);
        
        // === GŁÓWNY PANEL ZAWARTOŚCI ===
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS)); // Układanie pionowe
        contentPanel.add(headerPanel);
        contentPanel.add(instructionsPanel);
        contentPanel.add(buttonPanel);
        contentPanel.add(resultContainer);
        contentPanel.add(nextPanel);
        
        // Dodanie głównego panelu do okna
        add(contentPanel, BorderLayout.CENTER);
    }
    
    /**
     * Ustawia obsługę zdarzeń dla przycisków
     */
    private void setupEventListeners() {
        // Przycisk losowania litery
        rollButton.addActionListener(e -> rollLetter());
        
        // Przycisk następnego gracza - zamyka okno modalne
        nextButton.addActionListener(e -> {
            setVisible(false); // Ukrywa okno
            dispose(); // Zwalnia zasoby okna
        });
    }
    
    /**
     * Główna metoda losowania litery dla aktualnego gracza
     * Generuje losową literę, wybiera losowy cel i wyświetla wynik
     */
    private void rollLetter() {
        // === POBRANIE LOSOWEJ LITERY ===
        // Uzyskanie dostępu do głównego okna i wywołanie metody losowania
        RandomizerApp parent = (RandomizerApp) getOwner();
        char letter = parent.getRandomLetterForModal();
        
        // === WYBÓR LOSOWEGO CELU DO WYŚWIETLENIA ===
        // Tworzenie listy dostępnych celów (wszyscy oprócz aktualnego gracza)
        java.util.List<String> availableTargets = new java.util.ArrayList<>();
        for (String name : parent.getNames()) {
            if (!name.equals(playerName)) {
                availableTargets.add(name);
            }
        }
        
        // Wybór losowego celu lub domyślnej wartości
        String targetForDisplay = availableTargets.isEmpty() ? 
            "Inny Gracz" : 
            availableTargets.get(new Random().nextInt(availableTargets.size()));
        
        // === UTWORZENIE WYNIKU LOSOWANIA ===
        result = new RandomizerApp.PlayerResult(playerName, letter);
        
        // === WYŚWIETLENIE WYNIKU ===
        showResult(targetForDisplay, letter);
        
        // === AKTUALIZACJA INTERFEJSU ===
        rollButton.setEnabled(false); // Wyłączenie przycisku losowania
        nextButton.setEnabled(true); // Włączenie przycisku następnego gracza
        
        // Aktualizacja instrukcji z wynikiem
        instructionsLabel.setText(playerName + " wylosował " + targetForDisplay + " z literą " + letter + "! Kliknij 'Następny Gracz' aby kontynuować.");
    }
    
    /**
     * Wyświetla wynik losowania w panelu wyników z animacją
     * @param target nazwa wylosowanej osoby (do wyświetlenia)
     * @param letter wylosowana litera
     */
    private void showResult(String target, char letter) {
        resultPanel.removeAll(); // Czyszczenie poprzedniej zawartości
        
        // === ETYKIETA "KTO LOSUJE" ===
        JLabel drawerLabel = new JLabel(playerName + " losuje:");
        drawerLabel.setFont(drawerLabel.getFont().deriveFont(Font.BOLD, 14f));
        drawerLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Wyśrodkowanie
        drawerLabel.setForeground(new Color(73, 80, 87)); // Ciemnoszary kolor
        
        // === ETYKIETA Z NAZWĄ WYLOSOWANEJ OSOBY ===
        JLabel targetLabel = new JLabel(target);
        targetLabel.setFont(targetLabel.getFont().deriveFont(Font.BOLD, 24f)); // Duża czcionka
        targetLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        targetLabel.setForeground(new Color(21, 101, 192)); // Niebieski kolor
        // Stylowanie jako kolorowy panel
        targetLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(33, 150, 243), 2), // Niebieskie obramowanie
            BorderFactory.createEmptyBorder(8, 16, 8, 16) // Wewnętrzne odstępy
        ));
        targetLabel.setBackground(new Color(227, 242, 253)); // Jasnoniebiskie tło
        targetLabel.setOpaque(true);
        
        // === ETYKIETA Z WYLOSOWANĄ LITERĄ ===
        JLabel letterLabel = new JLabel(String.valueOf(letter));
        letterLabel.setFont(letterLabel.getFont().deriveFont(Font.BOLD, 36f)); // Bardzo duża czcionka
        letterLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        letterLabel.setForeground(new Color(255, 107, 107)); // Czerwony kolor
        
        // === DODANIE KOMPONENTÓW DO PANELU ===
        resultPanel.add(drawerLabel);
        resultPanel.add(Box.createVerticalStrut(8)); // Pionowy odstęp
        resultPanel.add(targetLabel);
        resultPanel.add(Box.createVerticalStrut(8)); // Pionowy odstęp
        resultPanel.add(letterLabel);
        
        // === WYŚWIETLENIE I ODŚWIEŻENIE PANELU ===
        resultPanel.setVisible(true);
        resultPanel.revalidate(); // Przeliczenie układu
        resultPanel.repaint(); // Odświeżenie wyświetlania
        
        // === PROSTA ANIMACJA POJAWIANIA SIĘ ===
        Timer timer = new Timer(100, new ActionListener() {
            private int step = 0; // Krok animacji
            
            @Override
            public void actionPerformed(ActionEvent e) {
                step++;
                if (step <= 10) {
                    // Proste odświeżanie przez 10 kroków (efekt fade-in)
                    resultPanel.repaint();
                } else {
                    // Zatrzymanie timera po zakończeniu animacji
                    ((Timer) e.getSource()).stop();
                }
            }
        });
        timer.start(); // Uruchomienie animacji
    }
    
    /**
     * Zwraca wynik losowania tego gracza
     * @return obiekt PlayerResult z imieniem gracza i wylosowaną literą
     */
    public RandomizerApp.PlayerResult getResult() {
        return result;
    }
}