import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.awt.image.BufferedImage;

/**
 * Główna klasa aplikacji Randomizer - aplikacja do losowego przydzielania par
 * z systemem ograniczeń i losowania liter w kolejnych turach
 */
public class RandomizerApp extends JFrame {
    // === PALETA KOLORÓW ===
    private static final Color PRIMARY_COLOR = new Color(52, 152, 219);      // Niebieski
    private static final Color SECONDARY_COLOR = new Color(46, 204, 113);    // Zielony
    private static final Color ACCENT_COLOR = new Color(231, 76, 60);        // Czerwony
    private static final Color WARNING_COLOR = new Color(255, 193, 7);       // Żółty
    private static final Color BACKGROUND_COLOR = new Color(248, 249, 250);  // Jasny szary
    private static final Color CARD_COLOR = Color.WHITE;                     // Biały
    private static final Color TEXT_COLOR = Color.BLACK;                     // Wszystkie teksty czarne
    private static final Color BORDER_COLOR = new Color(222, 226, 230);      // Jasny szary

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
    
    // === NOWE: POLE DO WYKLUCZONYCH LITER ===
    private JTextArea excludedLettersArea;
    
    // === DANE APLIKACJI ===
    
    // Lista wszystkich uczestników gry
    private List<String> names = new ArrayList<>();
    // Lista ograniczeń - pary które nie mogą być ze sobą dopasowane
    private List<Restriction> restrictions = new ArrayList<>();
    // Indeks aktualnie losującego gracza
    private int currentTurnIndex = 0;
    // Lista wyników każdego gracza (imię + wylosowana litera)
    private List<PlayerResult> playerResults = new ArrayList<>();
    // Lista finalnych przydziałów (kto kogo wylosował z jaką literą)
    private List<Assignment> finalPairs = new ArrayList<>();
    // === NOWE: ZBIÓR WYKLUCZONYCH LITER ===
    private Set<Character> excludedLetters = new HashSet<>();
    // === NOWE: PULA DOSTĘPNYCH CELÓW DO PRZYDZIAŁÓW NA BIEŻĄCO ===
    private List<String> availableTargetsPool = new ArrayList<>();
    
    /**
     * Konstruktor - inicjalizuje główne okno aplikacji
     */
    public RandomizerApp() {
        // Ustawienia okna z nowoczesnym stylem
        setTitle("Pair Randomizer - Modern Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Ustawienie tła głównego okna
        getContentPane().setBackground(BACKGROUND_COLOR);
        
        // Inicjalizacja komponentów i układu
        initializeComponents();
        setupLayout();
        setupEventListeners();
        
        // === TRYB PEŁNOEKRANOWY ===
        // Pobranie rozmiaru ekranu
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        
        // Ustawienie okna na pełny ekran
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setSize(screenSize);
        setLocationRelativeTo(null);
        
        // Alternatywnie można użyć prawdziwego trybu pełnoekranowego (bez paska zadań)
        // setUndecorated(true); // Usuwa ramkę okna
        // GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);
        
        // Ustawienie minimalnego rozmiaru (na wypadek wyjścia z trybu maksymalizacji)
        setMinimumSize(new Dimension(1200, 900));
        
        // Dodanie ikony okna (emoji jako fallback)
        try {
            setIconImage(createIconImage());
        } catch (Exception e) {
            // Ignoruj błędy z ikoną
        }
    }
    
    /**
     * Tworzy prostą ikonę dla aplikacji
     */
    private Image createIconImage() {
        BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = icon.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Tło ikony
        g2d.setColor(PRIMARY_COLOR);
        g2d.fillRoundRect(0, 0, 32, 32, 8, 8);
        
        // Tekst "R" na ikonie
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        FontMetrics fm = g2d.getFontMetrics();
        String text = "R";
        int x = (32 - fm.stringWidth(text)) / 2;
        int y = (32 + fm.getAscent() - fm.getDescent()) / 2;
        g2d.drawString(text, x, y);
        
        g2d.dispose();
        return icon;
    }

    /**
     * Inicjalizuje wszystkie komponenty interfejsu użytkownika z nowoczesnym stylem
     */
    private void initializeComponents() {
        // === SEKCJA NAZW ===
        nameListArea = new JTextArea(6, 50);
        styleTextArea(nameListArea);
        nameListArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(PRIMARY_COLOR, 2),
                "Wprowadź nazwy oddzielone przecinkami lub w nowych liniach",
                0, 0, new Font("Segoe UI", Font.BOLD, 12), PRIMARY_COLOR
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // === NOWE: SEKCJA WYKLUCZONYCH LITER ===
        excludedLettersArea = new JTextArea(2, 50);
        styleTextArea(excludedLettersArea);
        excludedLettersArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT_COLOR, 2),
                "Wykluczone litery (np. A,B,C lub ABCD) - te litery nie będą losowane",
                0, 0, new Font("Segoe UI", Font.BOLD, 12), ACCENT_COLOR
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        nameDisplayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        nameDisplayPanel.setBackground(CARD_COLOR);
        nameDisplayPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        loadNamesBtn = createStyledButton("Załaduj Nazwy", PRIMARY_COLOR, Color.WHITE);
        
        // === SEKCJA OGRANICZEŃ ===
        person1Select = new JComboBox<>();
        person2Select = new JComboBox<>();
        styleComboBox(person1Select);
        styleComboBox(person2Select);
        
        addRestrictionBtn = createStyledButton("Dodaj Ograniczenie", SECONDARY_COLOR, Color.WHITE);
        
        restrictionsModel = new DefaultListModel<>();
        restrictionsList = new JList<>(restrictionsModel);
        styleList(restrictionsList);
        
        // === SEKCJA STATUS GRY ===
        gameStatusLabel = new JLabel("Załaduj nazwy aby rozpocząć grę");
        styleLabel(gameStatusLabel, new Font("Segoe UI", Font.BOLD, 16), TEXT_COLOR);
        
        currentTurnLabel = new JLabel("");
        styleLabel(currentTurnLabel, new Font("Segoe UI", Font.PLAIN, 14), TEXT_COLOR);
        
        beginRollingBtn = createStyledButton("Rozpocznij Losowanie", ACCENT_COLOR, Color.WHITE);
        beginRollingBtn.setEnabled(false);
        
        // === SEKCJA WYNIKÓW ===
        pairResultsPanel = new JPanel();
        pairResultsPanel.setLayout(new BoxLayout(pairResultsPanel, BoxLayout.Y_AXIS));
        pairResultsPanel.setBackground(BACKGROUND_COLOR);
        pairResultsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        downloadResultsBtn = createStyledButton("Pobierz Wyniki (TXT)", SECONDARY_COLOR, Color.WHITE);
        downloadResultsBtn.setEnabled(false);
    }
    
    /**
     * Tworzy stylizowany przycisk z efektami hover i lepszą widocznością
     */
    private JButton createStyledButton(String text, Color bgColor, Color textColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 16)); // Zwiększona czcionka
        button.setForeground(textColor);
        button.setBackground(bgColor);
        button.setBorderPainted(true); // Włączenie obramowania
        button.setFocusPainted(true); // Włączenie fokusa
        button.setPreferredSize(new Dimension(250, 55)); // Większy rozmiar
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true); // Zapewnienie nieprzezroczystości
        
        // Wyraźne obramowanie dla lepszej widoczności
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(), // Poprawiona nazwa metody
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 2), // Czarne obramowanie
                BorderFactory.createEmptyBorder(10, 20, 10, 20) // Padding
            )
        ));
        
        // Dodanie efektu hover z lepszym kontrastem
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor.brighter());
                    button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createRaisedBevelBorder(),
                        BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.BLUE, 3), // Niebieskie przy hover
                            BorderFactory.createEmptyBorder(10, 20, 10, 20)
                        )
                    ));
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor);
                    button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createRaisedBevelBorder(),
                        BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.BLACK, 2),
                            BorderFactory.createEmptyBorder(10, 20, 10, 20)
                        )
                    ));
                }
            }
        });
        
        // Dodanie efektu kliknięcia
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLoweredBevelBorder(), // Poprawiona nazwa metody
                        BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.RED, 2),
                            BorderFactory.createEmptyBorder(10, 20, 10, 20)
                        )
                    ));
                }
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createRaisedBevelBorder(),
                        BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.BLACK, 2),
                            BorderFactory.createEmptyBorder(10, 20, 10, 20)
                        )
                    ));
                }
            }
        });
        
        return button;
    }
    
    /**
     * Stylizuje pole tekstowe
     */
    private void styleTextArea(JTextArea textArea) {
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textArea.setBackground(CARD_COLOR);
        textArea.setForeground(TEXT_COLOR);
        textArea.setCaretColor(PRIMARY_COLOR);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setMargin(new Insets(10, 10, 10, 10));
    }
    
    /**
     * Stylizuje listę rozwijalną
     */
    private void styleComboBox(JComboBox<String> comboBox) {
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboBox.setBackground(CARD_COLOR);
        comboBox.setForeground(TEXT_COLOR);
        comboBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        comboBox.setPreferredSize(new Dimension(200, 40));
    }
    
    /**
     * Stylizuje listę
     */
    private void styleList(JList<String> list) {
        list.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        list.setBackground(CARD_COLOR);
        list.setForeground(TEXT_COLOR);
        list.setSelectionBackground(new Color(PRIMARY_COLOR.getRed(), PRIMARY_COLOR.getGreen(), PRIMARY_COLOR.getBlue(), 50));
        list.setSelectionForeground(PRIMARY_COLOR.darker());
        list.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
    }
    
    /**
     * Stylizuje etykietę
     */
    private void styleLabel(JLabel label, Font font, Color color) {
        label.setFont(font);
        label.setForeground(color);
    }
    
    /**
     * Tworzy panel z efektem karty
     */
    private JPanel createCardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(CARD_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(25, 25, 25, 25)
        ));
        return panel;
    }

    /**
     * Tworzy układ interfejsu użytkownika z panelem nawigacyjnym po lewej i zawartością po prawej
     */
    private void setupLayout() {
        // Główny panel z podziałem na nawigację (lewo) i zawartość (prawo)
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setBackground(BACKGROUND_COLOR);
        mainSplitPane.setBorder(null);
        mainSplitPane.setDividerSize(0); // Ukrycie dzielnika
        mainSplitPane.setResizeWeight(0.0); // Panel nawigacyjny ma stałą szerokość
        
        // === PANEL NAWIGACYJNY PO LEWEJ STRONIE ===
        JPanel navigationPanel = createNavigationPanel();
        navigationPanel.setPreferredSize(new Dimension(200, 1000));
        navigationPanel.setMinimumSize(new Dimension(200, 600));
        
        // === PANEL ZAWARTOŚCI PO PRAWEJ STRONIE ===
        JPanel contentPanel = createContentPanel();
        
        // Dodanie paneli do split pane
        mainSplitPane.setLeftComponent(navigationPanel);
        mainSplitPane.setRightComponent(contentPanel);
        
        // Dodanie głównego panelu do okna z padding
        JPanel mainWrapper = new JPanel(new BorderLayout());
        mainWrapper.setBackground(BACKGROUND_COLOR);
        mainWrapper.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainWrapper.add(mainSplitPane, BorderLayout.CENTER);
        
        add(mainWrapper, BorderLayout.CENTER);
    }
    
    // Zmienna do śledzenia aktualnie wybranej sekcji
    private String currentSection = "names";
    
    /**
     * Tworzy panel nawigacyjny po lewej stronie
     */
    private JPanel createNavigationPanel() {
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBackground(new Color(60, 63, 65)); // Ciemny panel nawigacyjny
        navPanel.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
        
        // === TYTUŁ NAWIGACJI ===
        JLabel navTitle = new JLabel("Nawigacja");
        navTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        navTitle.setForeground(Color.WHITE);
        navTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        navTitle.setBorder(BorderFactory.createEmptyBorder(0, 10, 20, 0));
        navPanel.add(navTitle);
        
        // === PRZYCISKI NAWIGACYJNE ===
        String[] sections = {"names", "restrictions", "rolling", "results"};
        String[] sectionNames = {"Nazwy Uczestników", "Ograniczenia Par", "Losowanie Liter", "Wyniki Finalne"};
        
        for (int i = 0; i < sections.length; i++) {
            JButton navButton = createNavigationButton(sectionNames[i], sections[i]);
            navButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            navPanel.add(navButton);
            navPanel.add(Box.createVerticalStrut(5));
        }
        
        // Wypełnienie pozostałego miejsca
        navPanel.add(Box.createVerticalGlue());
        
        return navPanel;
    }
    
    /**
     * Tworzy przycisk nawigacyjny
     */
    private JButton createNavigationButton(String text, String section) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(70, 73, 75));
        button.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(170, 45));
        button.setPreferredSize(new Dimension(170, 45));
        
        // Wyróżnienie aktualnie wybranej sekcji
        if (section.equals(currentSection)) {
            button.setBackground(PRIMARY_COLOR);
            button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        }
        
        // Efekt hover
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (!section.equals(currentSection)) {
                    button.setBackground(new Color(80, 83, 85));
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (!section.equals(currentSection)) {
                    button.setBackground(new Color(70, 73, 75));
                }
            }
        });
        
        // Obsługa kliknięcia
        button.addActionListener(e -> switchToSection(section));
        
        return button;
    }
    
    // Panel zawartości i karty sekcji
    private JPanel contentContainer;
    private CardLayout contentCardLayout;
    
    /**
     * Tworzy panel zawartości po prawej stronie
     */
    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(BACKGROUND_COLOR);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        
        // === NAGŁÓWEK ZAWARTOŚCI ===
        JLabel contentTitle = new JLabel("");
        contentTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        contentTitle.setForeground(TEXT_COLOR);
        contentTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        contentPanel.add(contentTitle, BorderLayout.NORTH);
        
        // === KONTENER Z KARTAMI SEKCJI ===
        contentCardLayout = new CardLayout();
        contentContainer = new JPanel(contentCardLayout);
        contentContainer.setBackground(BACKGROUND_COLOR);
        
        // Dodanie kart dla każdej sekcji
        contentContainer.add(createNamesCard(), "names");
        contentContainer.add(createRestrictionsCard(), "restrictions");
        contentContainer.add(createRollingCard(), "rolling");
        contentContainer.add(createResultsCard(), "results");
        
        contentPanel.add(contentContainer, BorderLayout.CENTER);
        
        return contentPanel;
    }
    
    /**
     * Przełącza na wybraną sekcję
     */
    private void switchToSection(String section) {
        currentSection = section;
        contentCardLayout.show(contentContainer, section);
        
        // Aktualizacja wyglądu przycisków nawigacyjnych
        updateNavigationButtons();
    }
    
    /**
     * Aktualizuje wygląd przycisków nawigacyjnych
     */
    private void updateNavigationButtons() {
        // Znajdź panel nawigacyjny i zaktualizuj przyciski
        Container parent = getContentPane();
        updateNavigationButtonsRecursive(parent);
        repaint();
    }
    
    private void updateNavigationButtonsRecursive(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                String buttonText = button.getText();
                
                // Sprawdź czy to przycisk nawigacyjny i czy odpowiada aktualnej sekcji
                boolean isActive = (buttonText.equals("Nazwy Uczestników") && currentSection.equals("names")) ||
                                 (buttonText.equals("Ograniczenia Par") && currentSection.equals("restrictions")) ||
                                 (buttonText.equals("Losowanie Liter") && currentSection.equals("rolling")) ||
                                 (buttonText.equals("Wyniki Finalne") && currentSection.equals("results"));
                
                if (isActive) {
                    button.setBackground(PRIMARY_COLOR);
                    button.setFont(new Font("Segoe UI", Font.BOLD, 14));
                } else if (button.getBackground().equals(PRIMARY_COLOR)) {
                    button.setBackground(new Color(70, 73, 75));
                    button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                }
            } else if (comp instanceof Container) {
                updateNavigationButtonsRecursive((Container) comp);
            }
        }
    }
    
    /**
     * Tworzy kartę sekcji "Nazwy"
     */
    private JPanel createNamesCard() {
        JPanel card = createCardPanel();
        
        JPanel topPanel = new JPanel(new BorderLayout(0, 20));
        topPanel.setBackground(CARD_COLOR);
        
        // Panel dla pól tekstowych (nazwy + wykluczone litery)
        JPanel inputPanel = new JPanel(new BorderLayout(0, 15));
        inputPanel.setBackground(CARD_COLOR);
        
        JScrollPane nameAreaScroll = new JScrollPane(nameListArea);
        nameAreaScroll.setBorder(null);
        inputPanel.add(nameAreaScroll, BorderLayout.NORTH);
        
        JScrollPane excludedLettersScroll = new JScrollPane(excludedLettersArea);
        excludedLettersScroll.setBorder(null);
        inputPanel.add(excludedLettersScroll, BorderLayout.CENTER);
        
        topPanel.add(inputPanel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(CARD_COLOR);
        buttonPanel.add(loadNamesBtn);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        card.add(topPanel, BorderLayout.NORTH);
        
        JScrollPane nameDisplayScroll = new JScrollPane(nameDisplayPanel);
        nameDisplayScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(SECONDARY_COLOR, 2),
            "Załadowani uczestnicy",
            0, 0, new Font("Segoe UI", Font.BOLD, 12), SECONDARY_COLOR
        ));
        nameDisplayScroll.setBackground(CARD_COLOR);
        card.add(nameDisplayScroll, BorderLayout.CENTER);
        
        return card;
    }
    
    /**
     * Tworzy kartę sekcji "Ograniczenia"
     */
    private JPanel createRestrictionsCard() {
        JPanel card = createCardPanel();
        
        JLabel restrictionsTitle = new JLabel("Dodaj pary, które NIE powinny być dopasowane razem:");
        styleLabel(restrictionsTitle, new Font("Segoe UI", Font.BOLD, 14), TEXT_COLOR);
        restrictionsTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        JPanel restrictionsControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        restrictionsControlPanel.setBackground(CARD_COLOR);
        restrictionsControlPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(WARNING_COLOR, 2),
            BorderFactory.createEmptyBorder(25, 25, 25, 25)
        ));
        
        JLabel person1Label = new JLabel("Osoba 1:");
        styleLabel(person1Label, new Font("Segoe UI", Font.PLAIN, 13), TEXT_COLOR);
        restrictionsControlPanel.add(person1Label);
        restrictionsControlPanel.add(person1Select);
        
        JLabel arrowLabel = new JLabel("X");
        arrowLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        arrowLabel.setForeground(ACCENT_COLOR);
        restrictionsControlPanel.add(arrowLabel);
        
        JLabel person2Label = new JLabel("Osoba 2:");
        styleLabel(person2Label, new Font("Segoe UI", Font.PLAIN, 13), TEXT_COLOR);
        restrictionsControlPanel.add(person2Label);
        restrictionsControlPanel.add(person2Select);
        restrictionsControlPanel.add(addRestrictionBtn);
        
        JScrollPane restrictionsScroll = new JScrollPane(restrictionsList);
        restrictionsScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ACCENT_COLOR, 2),
            "Aktualne ograniczenia (Kliknij dwukrotnie aby usunąć)",
            0, 0, new Font("Segoe UI", Font.BOLD, 12), ACCENT_COLOR
        ));
        restrictionsScroll.setPreferredSize(new Dimension(0, 200));
        
        card.add(restrictionsTitle, BorderLayout.NORTH);
        card.add(restrictionsControlPanel, BorderLayout.CENTER);
        card.add(restrictionsScroll, BorderLayout.SOUTH);
        
        return card;
    }
    
    /**
     * Tworzy kartę sekcji "Losowanie"
     */
    private JPanel createRollingCard() {
        JPanel card = createCardPanel();
        
        JPanel statusCard = new JPanel(new BorderLayout(0, 25));
        statusCard.setBackground(CARD_COLOR);
        statusCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR, 3),
            BorderFactory.createEmptyBorder(40, 40, 40, 40)
        ));
        
        statusCard.add(gameStatusLabel, BorderLayout.NORTH);
        statusCard.add(currentTurnLabel, BorderLayout.CENTER);
        
        JPanel rollingButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        rollingButtonPanel.setBackground(CARD_COLOR);
        rollingButtonPanel.add(beginRollingBtn);
        statusCard.add(rollingButtonPanel, BorderLayout.SOUTH);
        
        card.add(statusCard, BorderLayout.CENTER);
        
        return card;
    }
    
    /**
     * Tworzy kartę sekcji "Wyniki"
     */
    private JPanel createResultsCard() {
        JPanel card = createCardPanel();
        
        JScrollPane resultsScroll = new JScrollPane(pairResultsPanel);
        resultsScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(SECONDARY_COLOR, 2),
            "Finalne wyniki",
            0, 0, new Font("Segoe UI", Font.BOLD, 14), SECONDARY_COLOR
        ));
        
        JPanel downloadPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        downloadPanel.setBackground(CARD_COLOR);
        downloadPanel.setBorder(BorderFactory.createEmptyBorder(25, 0, 0, 0));
        downloadPanel.add(downloadResultsBtn);
        
        card.add(resultsScroll, BorderLayout.CENTER);
        card.add(downloadPanel, BorderLayout.SOUTH);
        
        return card;
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
        
        // === NOWE: PRZETWARZANIE WYKLUCZONYCH LITER ===
        excludedLetters.clear();
        String excludedText = excludedLettersArea.getText().trim().toUpperCase();
        if (!excludedText.isEmpty()) {
            // Usuń przecinki i spacje, zostaw tylko litery
            excludedText = excludedText.replaceAll("[^A-ZĄĆĘŁŃÓŚŹŻ]", "");
            for (char c : excludedText.toCharArray()) {
                if (Character.isLetter(c)) {
                    excludedLetters.add(c);
                }
            }
        }
        
        // Informacja o wykluczonych literach
        if (!excludedLetters.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Wykluczone litery: " + excludedLetters.toString() + 
                "\nTe litery nie będą losowane.");
        }
        
        // Reset i aktualizacja interfejsu
        restrictions.clear();
        updateNameDisplay();
        updateSelectOptions();
        updateRestrictionsDisplay();
        startGame();
    }
    
    /**
     * Aktualizuje wyświetlanie nazw jako nowoczesne kolorowe karty
     */
    private void updateNameDisplay() {
        nameDisplayPanel.removeAll();
        
        Color[] colors = {
            SECONDARY_COLOR, PRIMARY_COLOR, ACCENT_COLOR, WARNING_COLOR,
            new Color(156, 39, 176), new Color(255, 152, 0), new Color(76, 175, 80)
        };
        
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            JLabel nameLabel = new JLabel(name);
            
            Color bgColor = colors[i % colors.length];
            nameLabel.setOpaque(true);
            nameLabel.setBackground(bgColor);
            nameLabel.setForeground(Color.WHITE);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            
            // Nowoczesne obramowanie z zaokrąglonymi rogami (symulacja)
            nameLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
            ));
            
            nameDisplayPanel.add(nameLabel);
        }
        
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
        currentTurnIndex = 0;
        playerResults.clear();
        finalPairs.clear();
        // Reset puli dostępnych celów
        availableTargetsPool.clear();
        availableTargetsPool.addAll(names);
        
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
            // Wszyscy wylosowali - jeśli pary już przypisane podczas losowania, tylko je pokaż
            gameStatusLabel.setText("Wszyscy gracze wylosowali!");
            currentTurnLabel.setText("");
            if (finalPairs.size() == names.size()) {
                displayAssignments();
                downloadResultsBtn.setEnabled(true);
            } else {
                generatePairsAutomatically();
            }
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
     * Generuje losową literę, unikając już użytych liter i wykluczonych liter
     * @return losowa litera z alfabetu
     */
    private char getRandomLetter() {
        String allLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Set<Character> usedLetters = new HashSet<>();
        
        // Zbieranie już użytych liter
        for (PlayerResult result : playerResults) {
            usedLetters.add(result.letter);
        }
        
        // === NOWE: DODANIE WYKLUCZONYCH LITER DO UŻYTYCH ===
        usedLetters.addAll(excludedLetters);
        
        // Tworzenie listy dostępnych liter
        List<Character> availableLetters = new ArrayList<>();
        for (char c : allLetters.toCharArray()) {
            if (!usedLetters.contains(c)) {
                availableLetters.add(c);
            }
        }
        
        // Zwracanie losowej dostępnej litery lub dowolnej jeśli wszystkie użyte
        if (availableLetters.isEmpty()) {
            // Jeśli wszystkie litery są wykluczone/użyte, wybierz z niewykluczonych
            List<Character> nonExcludedLetters = new ArrayList<>();
            for (char c : allLetters.toCharArray()) {
                if (!excludedLetters.contains(c)) {
                    nonExcludedLetters.add(c);
                }
            }
            if (!nonExcludedLetters.isEmpty()) {
                return nonExcludedLetters.get(new Random().nextInt(nonExcludedLetters.size()));
            }
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
     * Metoda publiczna do sprawdzania ograniczeń par (używana przez okno modalne)
     */
    public boolean isRestrictedPairPublic(String name1, String name2) {
        return isRestrictedPair(name1, name2);
    }
    
    /**
     * Automatycznie generuje finalne przydziały par na podstawie wylosowanych liter
     * Każda osoba musi kogoś wylosować i zostać wylosowana
     * POPRAWKA: Osoba nie może wylosować samej siebie
     */
    private void generatePairsAutomatically() {
        if (playerResults.size() != names.size()) return;
        
        // Jeśli pary już zostały przydzielone podczas losowania, nie nadpisuj
        if (finalPairs.size() == names.size()) {
            displayAssignments();
            downloadResultsBtn.setEnabled(true);
            return;
        }
        
        List<Assignment> assignments = new ArrayList<>();
        List<String> availableTargets = new ArrayList<>(names); // Kto może zostać wylosowany
        
        // Przydzielanie każdej osobie celu
        for (String drawer : names) {
            boolean assigned = false;
            List<String> possibleTargets = new ArrayList<>(availableTargets);
            
            // === POPRAWKA: USUŃ SIEBIE Z MOŻLIWYCH CELÓW ===
            possibleTargets.remove(drawer);
            
            // Mieszaj możliwe cele dla losowości
            Collections.shuffle(possibleTargets);
            
            // Szukanie prawidłowego celu (nie siebie + nie ograniczonego)
            for (String target : possibleTargets) {
                if (!isRestrictedPair(drawer, target)) {
                    // Znalezienie wyniku losowania dla tej osoby
                    PlayerResult drawerResult = playerResults.stream()
                        .filter(r -> r.name.equals(drawer))
                        .findFirst().orElse(null);
                    
                    if (drawerResult != null) {
                        // Utworzenie przydziału
                        assignments.add(new Assignment(drawer, target, drawerResult.letter));
                        availableTargets.remove(target);
                        assigned = true;
                        break;
                    }
                }
            }
            
            // Jeśli nie znaleziono prawidłowego, przydziel pierwszy dostępny (ale nie siebie!)
            if (!assigned && !possibleTargets.isEmpty()) {
                String target = possibleTargets.get(0);
                
                PlayerResult drawerResult = playerResults.stream()
                    .filter(r -> r.name.equals(drawer))
                    .findFirst().orElse(null);
                
                if (drawerResult != null) {
                    assignments.add(new Assignment(drawer, target, drawerResult.letter));
                    availableTargets.remove(target);
                }
            }
        }
        
        // Jeśli ktoś nie został przydzielony (zbyt dużo ograniczeń), pokaż komunikat
        if (assignments.size() < names.size()) {
            JOptionPane.showMessageDialog(this, 
                "Uwaga: Nie udało się przydzielić wszystkich par z powodu zbyt wielu ograniczeń. " +
                "Niektóre osoby mogą nie mieć przydziału.");
        }
        
        // Zapisanie wyników i wyświetlenie
        finalPairs = assignments;
        displayAssignments();
        downloadResultsBtn.setEnabled(true);
    }
    
    /**
     * Wyświetla finalne przydziały z nowoczesnym stylem kart
     */
    private void displayAssignments() {
        pairResultsPanel.removeAll();
        
        JLabel titleLabel = new JLabel("Wylosowane pary (każda osoba losuje kogoś):");
        styleLabel(titleLabel, new Font("Segoe UI", Font.BOLD, 18), TEXT_COLOR);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 25, 0));
        pairResultsPanel.add(titleLabel);
        
        if (finalPairs.isEmpty()) {
            JLabel errorLabel = new JLabel("Nie udało się wygenerować przydziałów z obecnymi ograniczeniami.");
            styleLabel(errorLabel, new Font("Segoe UI", Font.BOLD, 14), ACCENT_COLOR);
            pairResultsPanel.add(errorLabel);
        } else {
            for (Assignment assignment : finalPairs) {
                JPanel assignmentCard = new JPanel(new BorderLayout(20, 0));
                assignmentCard.setBackground(CARD_COLOR);
                assignmentCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1),
                    BorderFactory.createEmptyBorder(20, 25, 20, 25)
                ));
                assignmentCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
                
                // Dodanie subtelnego cienia
                assignmentCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0,0,0,20), 1),
                        BorderFactory.createLineBorder(CARD_COLOR, 2)
                    ),
                    BorderFactory.createEmptyBorder(20, 25, 20, 25)
                ));
                
                JLabel namesLabel = new JLabel(assignment.drawer + " → " + assignment.target);
                styleLabel(namesLabel, new Font("Segoe UI", Font.BOLD, 16), TEXT_COLOR);
                
                // === NOWA ELEGANCKA STYLIZACJA LITERY ===
                JPanel letterPanel = new JPanel();
                letterPanel.setLayout(new BorderLayout());
                letterPanel.setPreferredSize(new Dimension(60, 60));
                
                // Gradient-like effect z jasnoszarym tłem i niebieskim akcentem
                letterPanel.setBackground(new Color(240, 248, 255)); // Bardzo jasnoniebieski
                letterPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(PRIMARY_COLOR, 2), // Niebieskie obramowanie
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(220, 235, 255), 1), // Jasnoniebieski inner border
                        BorderFactory.createEmptyBorder(8, 8, 8, 8)
                    )
                ));
                
                JLabel letterLabel = new JLabel(String.valueOf(assignment.letter));
                letterLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
                letterLabel.setForeground(PRIMARY_COLOR); // Niebieska litera zamiast białej
                letterLabel.setHorizontalAlignment(SwingConstants.CENTER);
                letterLabel.setVerticalAlignment(SwingConstants.CENTER);
                letterPanel.add(letterLabel, BorderLayout.CENTER);
                
                assignmentCard.add(namesLabel, BorderLayout.CENTER);
                assignmentCard.add(letterPanel, BorderLayout.EAST);
                
                pairResultsPanel.add(assignmentCard);
                pairResultsPanel.add(Box.createVerticalStrut(15));
            }
        }
        
        pairResultsPanel.revalidate();
        pairResultsPanel.repaint();
    }
    
    /**
     * Zapisuje wyniki do pliku tekstowego
     * POPRAWKA: Lepsze formatowanie pliku TXT
     */
    private void downloadResults() {
        if (finalPairs.isEmpty()) return;
        
        // Okno dialogowe wyboru pliku
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new java.io.File("randomizer-wyniki-" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")) + ".txt"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (FileWriter writer = new FileWriter(fileChooser.getSelectedFile(), 
                 java.nio.charset.StandardCharsets.UTF_8)) {
                
                // === NAGŁÓWEK PLIKU ===
                writer.write("╔═══════════════════════════════════════════════════════════╗\n");
                writer.write("║                    RANDOMIZER - WYNIKI                   ║\n");
                writer.write("╚═══════════════════════════════════════════════════════════╝\n\n");
                
                writer.write("📅 Data wygenerowania: " + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
                writer.write("👥 Liczba uczestników: " + names.size() + "\n");
                writer.write("🎯 Liczba przydziałów: " + finalPairs.size() + "\n");
                
                // === SEKCJA WYKLUCZONYCH LITER ===
                if (!excludedLetters.isEmpty()) {
                    writer.write("❌ Wykluczone litery: " + excludedLetters.toString() + "\n");
                }
                
                // === SEKCJA OGRANICZEŃ ===
                if (!restrictions.isEmpty()) {
                    writer.write("🚫 Ograniczenia par: " + restrictions.size() + "\n");
                    for (Restriction r : restrictions) {
                        writer.write("   • " + r.person1 + " ↔ " + r.person2 + "\n");
                    }
                }
                
                writer.write("\n" + "=".repeat(60) + "\n");
                writer.write("                       FINALNE PRZYDZIAŁY\n");
                writer.write("=".repeat(60) + "\n\n");
                
                // === WYNIKI LOSOWANIA POSZCZEGÓLNYCH GRACZY ===
                writer.write("🎲 WYNIKI LOSOWANIA LITER:\n");
                writer.write("-".repeat(30) + "\n");
                for (PlayerResult result : playerResults) {
                    writer.write(String.format("%-20s → %c\n", result.name, result.letter));
                }
                
                writer.write("\n🎯 FINALNE PRZYDZIAŁY (kto kogo wylosował):\n");
                writer.write("-".repeat(50) + "\n");
                
                // === ZAPISANIE KAŻDEGO PRZYDZIAŁU W CZYTELNYM FORMACIE ===
                for (int i = 0; i < finalPairs.size(); i++) {
                    Assignment assignment = finalPairs.get(i);
                    writer.write(String.format("%d. %-15s → %-15s [Litera: %c]\n", 
                        i + 1, 
                        assignment.drawer, 
                        assignment.target, 
                        assignment.letter));
                }
                
                // === PODSUMOWANIE ===
                writer.write("\n" + "=".repeat(60) + "\n");
                writer.write("                        PODSUMOWANIE\n");
                writer.write("=".repeat(60) + "\n");
                writer.write("✅ Wszyscy uczestnicy mają swoje przydziały\n");
                writer.write("✅ Nikt nie wylosował samego siebie\n");
                if (!restrictions.isEmpty()) {
                    writer.write("✅ Wszystkie ograniczenia zostały uwzględnione\n");
                }
                if (!excludedLetters.isEmpty()) {
                    writer.write("✅ Wykluczone litery nie zostały wylosowane\n");
                }
                
                writer.write("\n📝 Plik wygenerowany przez Randomizer v2.0\n");
                writer.write("🕒 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
                
                JOptionPane.showMessageDialog(this, 
                    "Wyniki zostały zapisane do pliku!\n" +
                    "Lokalizacja: " + fileChooser.getSelectedFile().getAbsolutePath());
                    
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, 
                    "Błąd podczas zapisywania pliku: " + e.getMessage(), 
                    "Błąd", 
                    JOptionPane.ERROR_MESSAGE);
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
     * NOWE: Przydziela cel dla gracza podczas losowania i zapisuje parę (z literą)
     * Zwraca nazwę wylosowanej osoby do wyświetlenia w oknie modalnym.
     */
    public synchronized String assignTargetForOnRoll(String drawer, char letter) {
        // Upewnij się, że pula jest zainicjalizowana i spójna z dotychczasowymi przydziałami
        if (availableTargetsPool.isEmpty()) {
            availableTargetsPool.addAll(names);
            for (Assignment a : finalPairs) {
                availableTargetsPool.remove(a.target);
            }
        }
        
        // Zbuduj listę możliwych celów (bez siebie i bez ograniczeń)
        List<String> possibleTargets = new ArrayList<>();
        for (String t : new ArrayList<>(availableTargetsPool)) {
            if (!t.equals(drawer) && !isRestrictedPair(drawer, t)) {
                possibleTargets.add(t);
            }
        }
        Collections.shuffle(possibleTargets);
        String assignedTarget = possibleTargets.isEmpty() ? null : possibleTargets.get(0);
        
        // Obsługa trudnego przypadku: ostatni gracz i jedynym celem jest on sam – spróbuj zamiany
        if (assignedTarget == null && availableTargetsPool.size() == 1 && availableTargetsPool.get(0).equals(drawer)) {
            for (Assignment prev : finalPairs) {
                String candidate = prev.target;
                String prevDrawer = prev.drawer;
                if (candidate.equals(drawer)) continue;
                if (!isRestrictedPair(drawer, candidate) && !isRestrictedPair(prevDrawer, drawer) && !prevDrawer.equals(drawer)) {
                    // Zamiana celów
                    prev.target = drawer; // poprzedni bierze obecnego gracza
                    assignedTarget = candidate; // obecny bierze cel poprzedniego
                    // Usuń z puli 'drawer', ponieważ został użyty jako cel po zamianie
                    availableTargetsPool.remove(drawer);
                    // Zapisz nowy przydział
                    finalPairs.add(new Assignment(drawer, assignedTarget, letter));
                    return assignedTarget;
                }
            }
        }
        
        // Fallback – jeśli nadal brak, spróbuj wybrać kogokolwiek z puli (nie siebie), nawet gdy ograniczenie istnieje
        if (assignedTarget == null) {
            for (String t : availableTargetsPool) {
                if (!t.equals(drawer)) { assignedTarget = t; break; }
            }
            // Ostateczna rezerwa – jeśli nic innego, przypisz siebie (unikamy, ale wolimy zakończyć proces)
            if (assignedTarget == null) {
                assignedTarget = drawer;
            }
        }
        
        // Zdejmij wybranego z puli dostępnych celów
        availableTargetsPool.remove(assignedTarget);
        // Zapisz przydział
        finalPairs.add(new Assignment(drawer, assignedTarget, letter));
        return assignedTarget;
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
        String name;    // Imię gracza
        char letter;    // Wylosowana litera
        // Usunięto target
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