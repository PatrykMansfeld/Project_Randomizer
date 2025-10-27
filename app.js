// DOM Elements
const nameListTextarea = document.getElementById('nameList');
const loadNamesBtn = document.getElementById('loadNames');
const nameDisplay = document.getElementById('nameDisplay');
const person1Select = document.getElementById('person1Select');
const person2Select = document.getElementById('person2Select');
const addRestrictionBtn = document.getElementById('addRestriction');
const restrictionsList = document.getElementById('restrictionsList');
const gameStatus = document.getElementById('gameStatus');
const currentTurn = document.getElementById('currentTurn');
const beginRollingBtn = document.getElementById('beginRolling');
const progressTracker = document.getElementById('progressTracker');
const pairResults = document.getElementById('pairResults');
const downloadResultsBtn = document.getElementById('downloadResults');

// Modal elements
const rollingModal = document.getElementById('rollingModal');
const modalPlayerName = document.getElementById('modalPlayerName');
const modalTurnInfo = document.getElementById('modalTurnInfo');
const modalInstructions = document.getElementById('modalInstructions');
const modalRollBtn = document.getElementById('modalRollBtn');
const modalResult = document.getElementById('modalResult');
const modalNextBtn = document.getElementById('modalNextBtn');

// Game State
let names = [];
let restrictions = [];
let currentTurnIndex = 0;
let playerResults = [];
let gameInProgress = false;
let finalPairs = [];

// Utility Functions
function parseNames(text) {
    if (!text.trim()) return [];
    return text.split(/[,\n]/)
               .map(name => name.trim())
               .filter(name => name.length > 0);
}

function getRandomLetter() {
    // Get letters that haven't been used yet
    const allLetters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    const usedLetters = playerResults.map(r => r.letter);
    const availableLetters = allLetters.split('').filter(letter => !usedLetters.includes(letter));
    
    // If no letters available, something went wrong
    if (availableLetters.length === 0) {
        console.error('No more letters available!');
        return allLetters[Math.floor(Math.random() * allLetters.length)];
    }
    
    return availableLetters[Math.floor(Math.random() * availableLetters.length)];
}

function updateSelectOptions() {
    // Clear existing options
    person1Select.innerHTML = '<option value="">Select Person 1</option>';
    person2Select.innerHTML = '<option value="">Select Person 2</option>';
    
    // Add names to both selects
    names.forEach(name => {
        const option1 = document.createElement('option');
        option1.value = name;
        option1.textContent = name;
        person1Select.appendChild(option1);
        
        const option2 = document.createElement('option');
        option2.value = name;
        option2.textContent = name;
        person2Select.appendChild(option2);
    });
}

function displayNames() {
    nameDisplay.innerHTML = '';
    names.forEach(name => {
        const nameTag = document.createElement('div');
        nameTag.className = 'name-tag';
        nameTag.textContent = name;
        nameDisplay.appendChild(nameTag);
    });
}

function addRestriction(person1, person2) {
    if (!person1 || !person2 || person1 === person2) return false;
    
    // Check if restriction already exists
    const exists = restrictions.some(r => 
        (r.person1 === person1 && r.person2 === person2) ||
        (r.person1 === person2 && r.person2 === person1)
    );
    
    if (exists) return false;
    
    restrictions.push({ person1, person2 });
    return true;
}

function displayRestrictions() {
    restrictionsList.innerHTML = '';
    restrictions.forEach((restriction, index) => {
        const item = document.createElement('div');
        item.className = 'restriction-item';
        
        const text = document.createElement('span');
        text.className = 'restriction-text';
        text.textContent = `${restriction.person1} ↔ ${restriction.person2}`;
        
        const removeBtn = document.createElement('button');
        removeBtn.className = 'remove-restriction';
        removeBtn.textContent = 'Remove';
        removeBtn.onclick = () => removeRestriction(index);
        
        item.appendChild(text);
        item.appendChild(removeBtn);
        restrictionsList.appendChild(item);
    });
}

function removeRestriction(index) {
    restrictions.splice(index, 1);
    displayRestrictions();
}

function startGame() {
    if (names.length < 2) return;
    
    gameInProgress = true;
    currentTurnIndex = 0;
    playerResults = [];
    finalPairs = [];
    
    beginRollingBtn.disabled = false;
    downloadResultsBtn.disabled = true;
    
    updateGameStatus();
    updateProgressTracker();
}

function updateGameStatus() {
    if (currentTurnIndex < names.length) {
        gameStatus.textContent = `Ready to begin rolling - ${names.length} players total`;
        currentTurn.textContent = `Click "Begin Rolling" to start the turn-based rolling`;
        currentTurn.style.display = 'block';
    } else {
        gameStatus.textContent = 'All players have rolled! Generating pairs...';
        currentTurn.style.display = 'none';
        generatePairsAutomatically();
    }
}

function updateProgressTracker() {
    progressTracker.innerHTML = '<h3>Progress Tracker:</h3>';
    
    names.forEach((name, index) => {
        const item = document.createElement('div');
        item.className = 'progress-item';
        
        if (index < currentTurnIndex) {
            item.classList.add('completed');
            const result = playerResults.find(r => r.name === name);
            item.innerHTML = `<span>${name}</span><span>Rolled: ${result ? result.letter : '?'}</span>`;
        } else if (index === currentTurnIndex) {
            item.classList.add('current');
            item.innerHTML = `<span>${name}</span><span>Current Turn</span>`;
        } else {
            item.innerHTML = `<span>${name}</span><span>Waiting...</span>`;
        }
        
        progressTracker.appendChild(item);
    });
}

function showModal() {
    const currentPlayer = names[currentTurnIndex];
    modalPlayerName.textContent = `${currentPlayer}'s Turn`;
    modalTurnInfo.textContent = `Turn ${currentTurnIndex + 1} of ${names.length}`;
    modalInstructions.textContent = `${currentPlayer}, click "Roll Letter" to get your letter`;
    
    modalResult.textContent = '';
    modalResult.classList.remove('show');
    modalRollBtn.disabled = false;
    modalNextBtn.disabled = true;
    
    rollingModal.classList.add('show');
}

function hideModal() {
    rollingModal.classList.remove('show');
}

function rollLetterInModal() {
    const letter = getRandomLetter();
    const currentPlayer = names[currentTurnIndex];
    
    // Store result
    playerResults.push({
        name: currentPlayer,
        letter: letter
    });
    
    // Find who this player will draw (we need to generate this on the fly for display)
    const availableTargets = names.filter(name => name !== currentPlayer);
    const targetForDisplay = availableTargets[Math.floor(Math.random() * availableTargets.length)];
    
    // Show result with animation - include drawer, target, and letter
    modalResult.innerHTML = `
        <div class="result-drawer">${currentPlayer} losuje:</div>
        <div class="result-target">${targetForDisplay}</div>
        <div class="result-letter">${letter}</div>
    `;
    modalResult.classList.add('show');
    
    // Update button states
    modalRollBtn.disabled = true;
    modalNextBtn.disabled = false;
    
    // Update instructions
    modalInstructions.textContent = `${currentPlayer} wylosował ${targetForDisplay} z literą ${letter}! Kliknij "Next Player" aby kontynuować.`;
}

function nextPlayer() {
    currentTurnIndex++;
    hideModal();
    updateProgressTracker();
    
    if (currentTurnIndex < names.length) {
        // Continue to next player
        setTimeout(() => {
            showModal();
        }, 500);
    } else {    
        // All players done
        updateGameStatus();
    }
}

function isRestrictedPair(name1, name2) {
    return restrictions.some(r => 
        (r.person1 === name1 && r.person2 === name2) ||
        (r.person1 === name2 && r.person2 === name1)
    );
}

function generatePairsAutomatically() {
    if (playerResults.length !== names.length) return;
    
    // New system: Each person must be drawn and must draw someone
    // This means each person appears exactly twice in the system
    
    const assignments = [];
    const availableTargets = [...names]; // People who can still be assigned as targets
    const peopleWhoNeedToAssign = [...names]; // People who still need to draw someone
    
    // Shuffle both arrays for randomness
    for (let i = availableTargets.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [availableTargets[i], availableTargets[j]] = [availableTargets[j], availableTargets[i]];
    }
    
    for (let i = peopleWhoNeedToAssign.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [peopleWhoNeedToAssign[i], peopleWhoNeedToAssign[j]] = [peopleWhoNeedToAssign[j], peopleWhoNeedToAssign[i]];
    }
    
    // Assign each person to draw someone else
    peopleWhoNeedToAssign.forEach(drawer => {
        let assigned = false;
        
        // Try to find a valid target (not themselves, not restricted)
        for (let i = 0; i < availableTargets.length; i++) {
            const target = availableTargets[i];
            
            if (target !== drawer && !isRestrictedPair(drawer, target)) {
                // Get the drawer's letter
                const drawerResult = playerResults.find(r => r.name === drawer);
                
                assignments.push({
                    drawer: drawer,
                    target: target,
                    letter: drawerResult.letter
                });
                
                // Remove target from available list
                availableTargets.splice(i, 1);
                assigned = true;
                break;
            }
        }
        
        // If no valid target found due to restrictions, assign to first available
        if (!assigned && availableTargets.length > 0) {
            const target = availableTargets.find(t => t !== drawer) || availableTargets[0];
            const drawerResult = playerResults.find(r => r.name === drawer);
            
            assignments.push({
                drawer: drawer,
                target: target,
                letter: drawerResult.letter
            });
            
            const index = availableTargets.indexOf(target);
            if (index > -1) {
                availableTargets.splice(index, 1);
            }
        }
    });
    
    finalPairs = assignments;
    displayAssignments(assignments);
    downloadResultsBtn.disabled = false;
}

function displayAssignments(assignments) {
    pairResults.innerHTML = '<h3>Wylosowane Pary (Każda osoba losuje kogoś):</h3>';
    
    if (assignments.length === 0) {
        pairResults.innerHTML += '<p>Nie udało się wygenerować przydziałów z obecnymi ograniczeniami.</p>';
        return;
    }
    
    assignments.forEach(assignment => {
        const assignmentItem = document.createElement('div');
        assignmentItem.className = 'pair-item';
        
        const names = document.createElement('div');
        names.className = 'pair-names';
        names.textContent = `${assignment.drawer} → ${assignment.target}`;
        
        const letterDiv = document.createElement('div');
        letterDiv.className = 'pair-letter';
        letterDiv.textContent = assignment.letter;
        
        assignmentItem.appendChild(names);
        assignmentItem.appendChild(letterDiv);
        pairResults.appendChild(assignmentItem);
    });
}

function downloadTxtFile() {
    if (finalPairs.length === 0) return;
    
    let content = "RANDOMIZER WYNIKÓW - NOWY SYSTEM\n";
    content += "=================================\n\n";
    content += `Wygenerowano: ${new Date().toLocaleString()}\n`;
    content += `Liczba uczestników: ${names.length}\n`;
    content += `Liczba przydziałów: ${finalPairs.length}\n\n`;
    
    content += "SYSTEM: Każda osoba losuje kogoś i sama jest wylosowana\n";
    content += "Każda osoba pojawia się dokładnie 2 razy w systemie\n";
    content += "Żadna litera nie może się powtórzyć\n\n";
    
    if (restrictions.length > 0) {
        content += "ZASTOSOWANE OGRANICZENIA:\n";
        restrictions.forEach(r => {
            content += `- ${r.person1} ↔ ${r.person2}\n`;
        });
        content += "\n";
    }
    
    content += "PRZYDZIAŁY (KTO → KOGO WYLOSOWAŁ):\n";
    content += "==================================\n";
    
    finalPairs.forEach((assignment) => {
        content += `${assignment.drawer} → ${assignment.target} → ${assignment.letter}\n`;
    });
    
    content += "\n";
    content += "WYLOSOWANE LITERY:\n";
    content += "==================\n";
    playerResults.forEach(result => {
        content += `${result.name}: ${result.letter}\n`;
    });
    
    content += "\n";
    content += "PODSUMOWANIE - KTO ZOSTAŁ WYLOSOWANY:\n";
    content += "====================================\n";
    
    const targetCounts = {};
    finalPairs.forEach(assignment => {
        targetCounts[assignment.target] = (targetCounts[assignment.target] || 0) + 1;
    });
    
    names.forEach(name => {
        const count = targetCounts[name] || 0;
        content += `${name}: wylosowany ${count} ${count === 1 ? 'raz' : 'razy'}\n`;
    });
    
    // Create and download file
    const blob = new Blob([content], { type: 'text/plain' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `randomizer-nowy-system-${new Date().toISOString().slice(0, 10)}.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
}

// Event Listeners
loadNamesBtn.addEventListener('click', function() {
    const newNames = parseNames(nameListTextarea.value);
    
    if (newNames.length < 2) {
        alert('Please enter at least 2 names to start the game.');
        return;
    }
    
    names = newNames;
    restrictions = []; // Clear restrictions when loading new names
    
    displayNames();
    updateSelectOptions();
    displayRestrictions();
    startGame();
});

addRestrictionBtn.addEventListener('click', function() {
    const person1 = person1Select.value;
    const person2 = person2Select.value;
    
    if (!person1 || !person2) {
        alert('Please select both people for the restriction.');
        return;
    }
    
    if (person1 === person2) {
        alert('A person cannot be restricted from themselves.');
        return;
    }
    
    if (addRestriction(person1, person2)) {
        displayRestrictions();
        person1Select.value = '';
        person2Select.value = '';
    } else {
        alert('This restriction already exists.');
    }
});

beginRollingBtn.addEventListener('click', function() {
    beginRollingBtn.disabled = true;
    showModal();
});

modalRollBtn.addEventListener('click', rollLetterInModal);
modalNextBtn.addEventListener('click', nextPlayer);
downloadResultsBtn.addEventListener('click', downloadTxtFile);

// Close modal when clicking outside
rollingModal.addEventListener('click', function(e) {
    if (e.target === rollingModal) {
        // Don't allow closing modal during game
        if (!modalNextBtn.disabled) {
            alert('Please click "Next Player" to continue the game.');
        }
    }
});

// Initialize
gameStatus.textContent = 'Load names to start the game';
pairResults.textContent = 'Complete all turns to see final pairs';