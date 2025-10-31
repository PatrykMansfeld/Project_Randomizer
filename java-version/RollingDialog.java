import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;
import java.awt.image.BufferedImage;

/**
 * Klasa RollingDialog - okno modalne do losowania liter przez poszczególnych graczy
 * Wyświetla się dla każdego gracza w jego turze i pozwala na wylosowanie litery
 */
public class RollingDialog extends JDialog {
    // === PALETA KOLORÓW (zgodna z głównym oknem) ===
    private static final Color PRIMARY_COLOR = new Color(52, 152, 219);      // Niebieski
    private static final Color SECONDARY_COLOR = new Color(46, 204, 113);    // Zielony
    private static final Color ACCENT_COLOR = new Color(231, 76, 60);        // Czerwony
    private static final Color WARNING_COLOR = new Color(255, 193, 7);       // Żółty
    private static final Color BACKGROUND_COLOR = new Color(248, 249, 250);  // Jasny szary
    private static final Color CARD_COLOR = Color.WHITE;                     // Biały
    private static final Color TEXT_COLOR = Color.BLACK;                     // Wszystkie teksty czarne
    private static final Color BORDER_COLOR = new Color(222, 226, 230);      // Jasny szary

    // === DANE GRACZA I TURY ===
    
    // Imię aktualnie losującego gracza
    private String playerName;
    // Numer aktualnej tury (1, 2, 3...)
    private int turnNumber;
    // Całkowita liczba graczy w grze
    private int totalPlayers;
    // Wynik losowania tego gracza (imię + litera)
    private RandomizerApp.PlayerResult result;
    
    // === STATYCZNE ZMIENNE DO ZAPAMIĘTYWANIA ROZMIARU OKNA ===
    private static Dimension savedSize = null;
    private static Point savedLocation = null;
    
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
        super(parent, "Losowanie Litery - " + playerName, true);
        
        // Zapisanie parametrów
        this.playerName = playerName;
        this.turnNumber = turnNumber;
        this.totalPlayers = totalPlayers;
        
        // Ustawienie tła okna
        getContentPane().setBackground(BACKGROUND_COLOR);
        
        // Inicjalizacja komponentów i układu
        initializeComponents();
        setupLayout();
        setupEventListeners();
        
        // === TRYB PEŁNOEKRANOWY DLA OKNA MODALNEGO ===
        // Pobranie rozmiaru ekranu
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        
        // Przywrócenie zapisanego rozmiaru i pozycji lub ustawienie większego rozmiaru
        if (savedSize != null) {
            setSize(savedSize);
        } else {
            // Pierwsze otwarcie - ustaw na większy rozmiar (90% ekranu)
            int width = (int)(screenSize.width * 0.9);
            int height = (int)(screenSize.height * 0.9);
            setSize(width, height);
        }
        
        if (savedLocation != null) {
            setLocation(savedLocation);
        } else {
            setLocationRelativeTo(null); // Wycentrowanie na ekranie
        }
        
        // Zwiększenie minimalnego rozmiaru
        setMinimumSize(new Dimension(1200, 900));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(true);
        
        // Alternatywnie można użyć prawdziwego trybu pełnoekranowego
        // setUndecorated(true); // Usuwa ramkę okna
        // GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);
        
        // Dodanie listenera do zapisywania rozmiaru przy zmianie
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                // Zapisanie aktualnego rozmiaru
                savedSize = getSize();
            }
            
            @Override
            public void componentMoved(java.awt.event.ComponentEvent e) {
                // Zapisanie aktualnej pozycji
                savedLocation = getLocation();
            }
        });
        
        // Dodanie skrótu klawiszowego ESC do zamknięcia okna
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    setVisible(false);
                    dispose();
                }
            }
        });
        
        // Upewnienie się, że okno może otrzymywać zdarzenia klawiatury
        setFocusable(true);
        requestFocus();
    }
    
    /**
     * Inicjalizuje wszystkie komponenty okna modalnego z nowoczesnym stylem
     */
    private void initializeComponents() {
        // === NAGŁÓWEK Z IMIENIEM GRACZA ===
        playerLabel = new JLabel(playerName + " - Twoja Kolej!");
        playerLabel.setFont(new Font("Segoe UI", Font.BOLD, 32)); // Zwiększona czcionka
        playerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        playerLabel.setForeground(TEXT_COLOR);
        
        // === INFORMACJA O POSTĘPIE TURY ===
        turnInfoLabel = new JLabel("Tura " + turnNumber + " z " + totalPlayers);
        turnInfoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18)); // Zwiększona czcionka
        turnInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        turnInfoLabel.setForeground(TEXT_COLOR);
        
        // === INSTRUKCJE DLA GRACZA ===
        instructionsLabel = new JLabel("<html><center>" + playerName + ", kliknij przycisk poniżej<br>aby wylosować swoją literę!</center></html>");
        instructionsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18)); // Zwiększona czcionka
        instructionsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        instructionsLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR, 2),
            BorderFactory.createEmptyBorder(30, 40, 30, 40) // Zwiększone padding
        ));
        instructionsLabel.setBackground(new Color(PRIMARY_COLOR.getRed(), PRIMARY_COLOR.getGreen(), PRIMARY_COLOR.getBlue(), 30));
        instructionsLabel.setOpaque(true);
        instructionsLabel.setForeground(TEXT_COLOR);
        
        // === PRZYCISK LOSOWANIA z nowoczesnym stylem ===
        rollButton = createStyledButton("Losuj Literę!", ACCENT_COLOR, Color.WHITE, new Dimension(320, 80)); // Zwiększony rozmiar
        rollButton.setFont(new Font("Segoe UI", Font.BOLD, 22)); // Zwiększona czcionka
        
        // === PANEL WYNIKÓW (początkowo ukryty) ===
        resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.setPreferredSize(new Dimension(600, 250)); // Zwiększony rozmiar
        resultPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SECONDARY_COLOR, 3),
            BorderFactory.createEmptyBorder(40, 40, 40, 40) // Zwiększone padding
        ));
        resultPanel.setBackground(CARD_COLOR);
        resultPanel.setVisible(false);
        
        // === PRZYCISK NASTĘPNEGO GRACZA ===
        nextButton = createStyledButton("Następny Gracz", SECONDARY_COLOR, Color.WHITE, new Dimension(250, 60)); // Zwiększony rozmiar
        nextButton.setFont(new Font("Segoe UI", Font.BOLD, 20)); // Zwiększona czcionka
        nextButton.setEnabled(false);
    }
    
    /**
     * Tworzy stylizowany przycisk z efektami hover i lepszą widocznością (podobny do głównego okna)
     */
    private JButton createStyledButton(String text, Color bgColor, Color textColor, Dimension size) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 18)); // Większa czcionka
        button.setForeground(textColor);
        button.setBackground(bgColor);
        button.setBorderPainted(true); // Włączenie obramowania
        button.setFocusPainted(true); // Włączenie fokusa
        button.setPreferredSize(size);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true); // Zapewnienie nieprzezroczystości
        
        // Wyraźne obramowanie dla lepszej widoczności
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(), // Poprawiona nazwa metody
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 3), // Grubsze czarne obramowanie
                BorderFactory.createEmptyBorder(12, 25, 12, 25) // Większy padding
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
                            BorderFactory.createLineBorder(Color.BLUE, 4), // Niebieskie przy hover
                            BorderFactory.createEmptyBorder(12, 25, 12, 25)
                        )
                    ));
                    if (button == rollButton && button.isEnabled()) {
                        button.setText("Kliknij aby losować!");
                    }
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor);
                    button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createRaisedBevelBorder(),
                        BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.BLACK, 3),
                            BorderFactory.createEmptyBorder(12, 25, 12, 25)
                        )
                    ));
                    if (button == rollButton && button.isEnabled()) {
                        button.setText("Losuj Literę!");
                    }
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
                            BorderFactory.createLineBorder(Color.RED, 3),
                            BorderFactory.createEmptyBorder(12, 25, 12, 25)
                        )
                    ));
                }
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createRaisedBevelBorder(),
                        BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.BLACK, 3),
                            BorderFactory.createEmptyBorder(12, 25, 12, 25)
                        )
                    ));
                }
            }
        });
        
        return button;
    }

    /**
     * Tworzy układ komponentów w oknie modalnym z nowoczesnym stylem kart
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // === GŁÓWNA KARTA ZAWARTOŚCI ===
        JPanel mainCard = new JPanel();
        mainCard.setLayout(new BoxLayout(mainCard, BoxLayout.Y_AXIS));
        mainCard.setBackground(CARD_COLOR);
        mainCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(35, 45, 35, 45)
        ));
        
        // === SEKCJA NAGŁÓWKA ===
        JPanel headerSection = new JPanel();
        headerSection.setLayout(new BoxLayout(headerSection, BoxLayout.Y_AXIS));
        headerSection.setBackground(CARD_COLOR);
        headerSection.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        
        playerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        turnInfoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        headerSection.add(playerLabel);
        headerSection.add(Box.createVerticalStrut(12));
        headerSection.add(turnInfoLabel);
        
        // === SEKCJA INSTRUKCJI ===
        JPanel instructionsSection = new JPanel(new BorderLayout());
        instructionsSection.setBackground(CARD_COLOR);
        instructionsSection.setBorder(BorderFactory.createEmptyBorder(10, 0, 35, 0));
        instructionsSection.add(instructionsLabel, BorderLayout.CENTER);
        
        // === SEKCJA PRZYCISKU LOSOWANIA ===
        JPanel buttonSection = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonSection.setBackground(CARD_COLOR);
        buttonSection.setBorder(BorderFactory.createEmptyBorder(15, 0, 30, 0));
        buttonSection.add(rollButton);
        
        // === SEKCJA WYNIKÓW ===
        JPanel resultSection = new JPanel(new BorderLayout());
        resultSection.setBackground(CARD_COLOR);
        resultSection.setBorder(BorderFactory.createEmptyBorder(15, 0, 30, 0));
        resultSection.add(resultPanel, BorderLayout.CENTER);
        
        // === SEKCJA PRZYCISKU NASTĘPNEGO GRACZA ===
        JPanel nextSection = new JPanel(new FlowLayout(FlowLayout.CENTER));
        nextSection.setBackground(CARD_COLOR);
        nextSection.add(nextButton);
        
        // === SKŁADANIE WSZYSTKICH SEKCJI ===
        mainCard.add(headerSection);
        mainCard.add(instructionsSection);
        mainCard.add(buttonSection);
        mainCard.add(resultSection);
        mainCard.add(nextSection);
        
        // === WRAPPER Z TŁEM I PADDINGIEM ===
        JPanel backgroundWrapper = new JPanel(new BorderLayout());
        backgroundWrapper.setBackground(BACKGROUND_COLOR);
        backgroundWrapper.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        backgroundWrapper.add(mainCard, BorderLayout.CENTER);
        
        add(backgroundWrapper, BorderLayout.CENTER);
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
     * Główna metoda losowania litery dla aktualnego gracza z ulepszoną animacją
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
        
        // === AKTUALIZACJA INTERFEJSU Z ANIMACJĄ ===
        rollButton.setEnabled(false);
        rollButton.setText("Wylosowano!");
        rollButton.setBackground(SECONDARY_COLOR);
        nextButton.setEnabled(true);
        
        // Ulepszony tekst instrukcji z emoji i formatowaniem
        instructionsLabel.setText("<html><center>" + playerName + " wylosował " + targetForDisplay + "<br>z literą " + letter + "!<br><br>Kliknij 'Następny Gracz' aby kontynuować.</center></html>");
    }
    
    /**
     * Wyświetla wynik losowania w panelu wyników z nowoczesną animacją i stylem
     * @param target nazwa wylosowanej osoby (do wyświetlenia)
     * @param letter wylosowana litera
     */
    private void showResult(String target, char letter) {
        resultPanel.removeAll();
        
        // === TYTUŁ WYNIKU ===
        JLabel titleLabel = new JLabel("Wynik Losowania:");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setForeground(TEXT_COLOR);
        
        // === KARTA Z NAZWĄ WYLOSOWANEJ OSOBY ===
        JPanel targetCard = new JPanel(new BorderLayout());
        targetCard.setBackground(new Color(PRIMARY_COLOR.getRed(), PRIMARY_COLOR.getGreen(), PRIMARY_COLOR.getBlue(), 40));
        targetCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR, 2),
            BorderFactory.createEmptyBorder(18, 25, 18, 25)
        ));
        targetCard.setMaximumSize(new Dimension(450, 70));
        
        JLabel targetLabel = new JLabel(target);
        targetLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        targetLabel.setForeground(TEXT_COLOR);
        targetLabel.setHorizontalAlignment(SwingConstants.CENTER);
        targetCard.add(targetLabel, BorderLayout.CENTER);
        
        // === KARTA Z LITERĄ (główny element) ===
        JPanel letterCard = new JPanel(new BorderLayout());
        letterCard.setBackground(ACCENT_COLOR);
        letterCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_COLOR.darker(), 3),
            BorderFactory.createEmptyBorder(25, 25, 25, 25)
        ));
        letterCard.setMaximumSize(new Dimension(120, 100));
        
        JLabel letterLabel = new JLabel(String.valueOf(letter));
        letterLabel.setFont(new Font("Segoe UI", Font.BOLD, 52));
        letterLabel.setForeground(Color.WHITE);
        letterLabel.setHorizontalAlignment(SwingConstants.CENTER);
        letterCard.add(letterLabel, BorderLayout.CENTER);
        
        // === SKŁADANIE ELEMENTÓW WYNIKU ===
        resultPanel.add(titleLabel);
        resultPanel.add(Box.createVerticalStrut(20));
        resultPanel.add(targetCard);
        resultPanel.add(Box.createVerticalStrut(20));
        resultPanel.add(letterCard);
        
        // === WYŚWIETLENIE Z ANIMACJĄ ===
        resultPanel.setVisible(true);
        resultPanel.revalidate();
        resultPanel.repaint();
        
        // === NOWOCZESNA ANIMACJA FADE-IN ===
        animateResultAppearance(letterCard);
    }
    
    /**
     * Animuje pojawienie się wyniku z efektem bounce
     */
    private void animateResultAppearance(JPanel letterCard) {
        Timer fadeTimer = new Timer(40, new ActionListener() {
            private int step = 0;
            private final int maxSteps = 15;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                step++;
                float alpha = (float) step / maxSteps;
                
                // Efekt fade-in przez zmianę przezroczystości
                Color bgColor = resultPanel.getBackground();
                resultPanel.setBackground(new Color(
                    bgColor.getRed(), 
                    bgColor.getGreen(), 
                    bgColor.getBlue(), 
                    Math.min(255, (int)(alpha * 255))
                ));
                
                if (step >= maxSteps) {
                    ((Timer) e.getSource()).stop();
                    // Po fade-in uruchom efekt bounce na literze
                    animateBounce(letterCard);
                }
                
                resultPanel.repaint();
            }
        });
        fadeTimer.start();
    }
    
    /**
     * Animuje efekt bounce dla karty z literą
     */
    private void animateBounce(JPanel letterCard) {
        Timer bounceTimer = new Timer(120, new ActionListener() {
            private int bounces = 0;
            private boolean growing = true;
            private Dimension originalSize = letterCard.getPreferredSize();
            private final int maxBounces = 3;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (growing) {
                    // Powiększenie
                    letterCard.setPreferredSize(new Dimension(
                        (int)(originalSize.width * 1.15),
                        (int)(originalSize.height * 1.15)
                    ));
                    growing = false;
                } else {
                    // Powrót do normalnego rozmiaru
                    letterCard.setPreferredSize(originalSize);
                    growing = true;
                    bounces++;
                }
                
                letterCard.revalidate();
                letterCard.repaint();
                
                if (bounces >= maxBounces) {
                    ((Timer) e.getSource()).stop();
                    letterCard.setPreferredSize(originalSize);
                    letterCard.revalidate();
                    
                    // Dodaj subtelny efekt świecenia
                    addGlowEffect(letterCard);
                }
            }
        });
        bounceTimer.start();
    }
    
    /**
     * Dodaje subtelny efekt świecenia do karty z literą
     */
    private void addGlowEffect(JPanel letterCard) {
        Timer glowTimer = new Timer(100, new ActionListener() {
            private int glowStep = 0;
            private boolean increasing = true;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (increasing) {
                    glowStep++;
                    if (glowStep >= 10) increasing = false;
                } else {
                    glowStep--;
                    if (glowStep <= 0) increasing = true;
                }
                
                // Zmiana intensywności obramowania dla efektu świecenia
                int intensity = 100 + (glowStep * 15);
                Color glowColor = new Color(
                    Math.min(255, ACCENT_COLOR.getRed() + intensity/10),
                    Math.min(255, ACCENT_COLOR.getGreen() + intensity/10),
                    Math.min(255, ACCENT_COLOR.getBlue() + intensity/10)
                );
                
                letterCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(glowColor, 3),
                    BorderFactory.createEmptyBorder(25, 25, 25, 25)
                ));
                
                letterCard.repaint();
            }
        });
        
        // Uruchom efekt świecenia na 3 sekundy
        Timer stopGlowTimer = new Timer(3000, e -> glowTimer.stop());
        stopGlowTimer.setRepeats(false);
        stopGlowTimer.start();
        glowTimer.start();
    }
    
    /**
     * Zwraca wynik losowania tego gracza
     * @return obiekt PlayerResult z imieniem gracza i wylosowaną literą
     */
    public RandomizerApp.PlayerResult getResult() {
        return result;
    }
}