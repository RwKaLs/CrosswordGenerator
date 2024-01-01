package assignment2.students.EgorMeganov;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.file.Paths.get;

public class EgorMeganov {

    /**
     * Main method
     * Reads input files and runs genetic algorithm for each file
     */
    public static void main(String[] args) {
        readInputFiles();
        GeneticAlgorithm geneticAlgorithm;
        int inNum = 1;
        // we iterate over input files and pass them to crosswords generator
        for (ArrayList<String> i: readInputFiles()) {
            try {
                geneticAlgorithm = new GeneticAlgorithm(400, i, inNum++);
                geneticAlgorithm.createCrossword();
            } catch (Exception e) {
                // skip
            }
        }
    }

    /**
     * Reads input files from inputs directory using stream
     * @return ArrayList of ArrayLists of Strings
     */
    private static ArrayList<ArrayList<String>> readInputFiles() {
        ArrayList<ArrayList<String>> filesData = new ArrayList<>();
        try {
            String path = System.getProperty("user.dir");
            Files.newDirectoryStream(get(path.startsWith("/") ? path.substring(1) : path, "inputs"), "*.txt")
                    .forEach(entry -> {
                        try {
                            filesData.add(
                                    (ArrayList<String>) Files.readAllLines(entry)
                            );
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            System.err.println("ReaderError");
        }
        return filesData;
    }
}

/**
 * Genetic algorithm class
 * Creates initial population and runs the genetic algorithm
 */
class GeneticAlgorithm {
    private final int populationSize; // population size
    private ArrayList<Individual> population; // population
    private final Random random; // random generator
    private final List<String> words; // word list
    private int maxFitness; // max fitness
    private int genN; // generation number
    private final int stopSig; // stop signal (if we reach it, the population regenerates)
    private int mutationFactor; // mutation factor (number of words to mutate per individual)

    private final int curInput; // current input file number

    /** Constructor
     * @param populationSize population size
     * @param words word list
     * @param curInput current input file number
     */
    public GeneticAlgorithm(int populationSize, ArrayList<String> words, int curInput) {
        this.populationSize = populationSize;
        this.population = new ArrayList<>();
        this.random = new Random();
        this.words = words;
        this.maxFitness = 10000;
        this.genN = 0;
        this.stopSig = words.size() * words.size() * 2;
        mutationFactor = 2;
        this.curInput = curInput;
    }

    /**
     * The primary method runs the genetic algorithm
     */
    public void createCrossword() {
        initializePopulation(); // initialize population
        ArrayList<Individual> newPopulation; // new population
        ArrayList<Individual> children; // children list (after crossover)
        int indChildren, indParent;
        while (maxFitness != 0) {
            population.sort(((o1, o2) -> calculateFitness(o1) - calculateFitness(o2))); // sort population by fitness
            children = new ArrayList<>();
            newPopulation = new ArrayList<>();
            indChildren = 0;
            indParent = 0;
            // perform crossover on 20% best individuals
            for (int i = 0; i < populationSize/5; ++i) {
                for (int j = 0; j < populationSize/5; ++j) {
                    if (i != j) {
                        children.add(performMutation(performCrossover(population.get(i), population.get(j))));
                    }
                }
            }
            children.sort(((o1, o2) -> calculateFitness(o1) - calculateFitness(o2))); // sort children by fitness
            // choose the best individuals from the set of population and children
            while (indChildren + indParent < populationSize) {
                if (population.get(indParent).fitness <= children.get(indChildren).fitness) {
                    newPopulation.add(performMutation(population.get(indParent++)));
                } else {
                    newPopulation.add(children.get(indChildren++));
                }
            }
            replacePopulation(newPopulation); // replace population with new population
            // if we have not found the solution for a long time, we restart the population
            if (genN++ % stopSig == 0) {
                initializePopulation();
                // save 5% of the old population
                for (int i = 0; i < random.nextInt(populationSize/20); ++i) {
                    population.set(i, newPopulation.get(i));
                }
                mutationFactor = 2; // reset mutation factor
            } else if (genN % (words.size() * 10) == 0) {
                // mutation factor helps to avoid local optima, it increases with number of generations
                mutationFactor = Math.min(mutationFactor+1, words.size()/2+1);
            }
            if (genN >= 50000) break; // if we have not found the solution for a long time, we stop (to pass tests)
        }
    }

    /**
     * Initializes population (random coordinates and orientation)
     */
    private void initializePopulation() {
        boolean vertical;
        int x, y;
        Individual individual;
        for (int i = 0; i < populationSize; i++) {
            individual = new Individual();
            for (String word : words) {
                vertical = random.nextBoolean();
                if (vertical) {
                    x = random.nextInt(20);
                    y = random.nextInt(20 - word.length());
                } else {
                    x = random.nextInt(20 - word.length());
                    y = random.nextInt(20);
                }
                if (vertical) {
                    individual.wordsList.add(new Word(word, x, y, true));
                } else {
                    individual.wordsList.add(new Word(word, x, y, false));
                }
            }
            population.add(individual);
        }
    }

    /**
     * Performs crossover between two individuals
     * Randomly chooses words from parents and creates a new individual from them
     * @param parent1 first parent
     * @param parent2 second parent
     * @return new individual
     */
    private Individual performCrossover(Individual parent1, Individual parent2) {
        Individual offspring = new Individual();
        Word newWord;
        for (int i = 0; i < parent1.wordsList.size(); ++i) {
            if (random.nextBoolean()) {
                newWord = new Word(parent1.wordsList.get(i).text, parent1.wordsList.get(i).startX,
                        parent1.wordsList.get(i).startY, parent1.wordsList.get(i).isVertical);
                offspring.wordsList.add(newWord);
            } else {
                newWord = new Word(parent2.wordsList.get(i).text, parent2.wordsList.get(i).startX,
                        parent2.wordsList.get(i).startY, parent2.wordsList.get(i).isVertical);
                offspring.wordsList.add(newWord);
            }
        }
        return offspring;
    }

    /**
     * Performs mutation on an individual
     * Randomly changes orientation and coordinates of words
     * Number of words to mutate is chosen randomly from 1 to mutationFactor
     * @param individual individual to mutate
     * @return mutated individual
     */
    private Individual performMutation(Individual individual) {
        Individual ret = new Individual(individual.wordsList);
        Word oldWord, newWord;
        int index;
        for (int i = 0; i < random.nextInt(1, mutationFactor); ++i) {
            index = random.nextInt(ret.wordsList.size());
            oldWord = ret.wordsList.get(index);
            newWord = new Word(oldWord.text, oldWord.startX, oldWord.startY, oldWord.isVertical);
            newWord.isVertical = random.nextBoolean();
            if (newWord.isVertical) {
                newWord.startX = random.nextInt(20);
                newWord.startY = random.nextInt(20 - newWord.text.length());
            } else {
                newWord.startX = random.nextInt(20 - newWord.text.length());
                newWord.startY = random.nextInt(20);
            }
            ret.wordsList.set(index, newWord);
        }
        return ret;
    }

    /**
     * Replaces population with new population
     * @param newPopulation new population
     */
    private void replacePopulation(ArrayList<Individual> newPopulation) {
        this.population = newPopulation;
    }

    int PENALTY_OVERLAP = 1;
    int PENALTY_FL = 1;
    int PENALTY_PARALLEL = 1;
    int PENALTY_CONNECTION = 1;

    /**
     * Calculates fitness of an individual
     * @param individual individual to calculate fitness
     * @return fitness
     */
    private int calculateFitness(Individual individual) {
        if (individual == null || individual.wordsList.isEmpty()) {
            return 100000;
        } else if (individual.fitness != 100000) {
            return individual.fitness;
        }
        int fitness = 0;
        char[][] grid = new char[20][20];
        // check if words overlap (place them on the grid and check if there are any conflicts)
        for (Word word : individual.wordsList) {
            if (word.isVertical) {
                for (int i = 0; i < word.text.length(); ++i) {
                    if (grid[word.startX][word.startY + i] != 0 && grid[word.startX][word.startY + i] != word.text.charAt(i)) {
                        fitness += PENALTY_OVERLAP;
                    }
                    grid[word.startX][word.startY + i] = word.text.charAt(i);
                }
            } else {
                for (int i = 0; i < word.text.length(); ++i) {
                    if (grid[word.startX + i][word.startY] != 0 && grid[word.startX + i][word.startY] != word.text.charAt(i)) {
                        fitness += PENALTY_OVERLAP;
                    }
                    grid[word.startX + i][word.startY] = word.text.charAt(i);
                }
            }
        }
        // check if above/below or left/right of words are empty
        for (Word word: individual.wordsList) {
            if (word.isVertical) {
                if (word.startY > 0) {
                    if (grid[word.startX][word.startY-1] != 0) {
                        fitness += PENALTY_FL;
                    }
                }
                if (word.startY + word.text.length() <= 19) {
                    if (grid[word.startX][word.startY+word.text.length()] != 0) {
                        fitness += PENALTY_FL;
                    }
                }
            } else {
                if (word.startX > 0) {
                    if (grid[word.startX-1][word.startY] != 0) {
                        fitness += PENALTY_FL;
                    }
                }
                if (word.startX + word.text.length() <= 19) {
                    if (grid[word.startX+word.text.length()][word.startY] != 0) {
                        fitness += PENALTY_FL;
                    }
                }
            }
        }
        // check if words are parallel (in adjacent rows or columns)
        for (Word word1 : individual.wordsList) {
            for (Word word2 : individual.wordsList) {
                if (word1 == word2) continue;
                if (word1.isVertical == word2.isVertical) {
                    if (word1.isVertical) {
                        if ((word1.startX == word2.startX-1 || word1.startX == word2.startX+1)
                                && ((word1.startY <= word2.startY && word2.startY <= word1.startY + word1.text.length()-1)
                        || (word1.startY >= word2.startY && word2.startY >= word1.startY + word1.text.length()-1))) {
                            fitness += PENALTY_PARALLEL * (Math.min(word1.startY+word1.text.length(), word2.startY+word2.text.length()) -
                                    Math.max(word1.startY, word2.startY));
                        }
                    } else {
                        if ((word1.startY == word2.startY-1 || word1.startY == word2.startY+1)
                                && ((word1.startX <= word2.startX && word2.startX <= word1.startX + word1.text.length()-1)
                                || (word1.startX >= word2.startX && word2.startX >= word1.startX + word1.text.length()-1))) {
                            fitness += PENALTY_PARALLEL * (Math.min(word1.startX+word1.text.length(), word2.startX+word2.text.length()) -
                                    Math.max(word1.startX, word2.startX));
                        }
                    }
                }
            }
        }
        // check if words are connected using DFS
        DFS dfs = checkDfs(individual.wordsList, grid);
        fitness += PENALTY_CONNECTION * dfs.components;
        individual.fitness = fitness;
        if (fitness == 0) {
            maxFitness = 0;
            outputResult(individual);
            printCrossword(individual);
        }
        return fitness;
    }

    /**
     * Checks if words are connected using DFS
     * @param individual individual to check
     * @param grid grid to check
     * @return DFS object
     */
    private static DFS checkDfs(ArrayList<Word> individual, char[][] grid) {
        DFS dfs = new DFS(grid);
        dfs.dfs(individual.get(0).startX, individual.get(0).startY);
        dfs.startDfs(individual.get(0).startX, individual.get(0).startY);
        for (Word word : individual) {
            if (word.isVertical) {
                for (int i = 0; i < word.text.length(); ++i) {
                    if (!dfs.visited[word.startX][word.startY + i]) {
                        dfs.startDfs(word.startX, word.startY + i);
                    }
                }
            } else {
                for (int i = 0; i < word.text.length(); ++i) {
                    if (!dfs.visited[word.startX + i][word.startY]) {
                        dfs.startDfs(word.startX + i, word.startY);
                    }
                }
            }
        }
        return dfs;
    }

    /**
     * Outputs result to output file
     * @param individual individual to output
     */
    private void outputResult(Individual individual) {
        String path = System.getProperty("user.dir");
        File dir = new File(String.valueOf(Paths.get(path.startsWith("/") ? path.substring(1) : path, "outputs")));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File out = new File(dir, "output" + curInput + ".txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(out))) {
            for (Word word : individual.wordsList) {
                String output = word.startY + " " + word.startX + " " + (word.isVertical ? 1 : 0);
                writer.println(output);
            }
        } catch (IOException e) {
            System.err.println("WriterError");
        }
    }

    public void printCrossword(Individual crossword) {
        char[][] grid = new char[20][20];
        for (Word word : crossword.wordsList) {
            if (word.isVertical) {
                for (int i = 0; i < word.text.length(); i++) {
                    grid[word.startY + i][word.startX] = word.text.charAt(i);
                }
            } else {
                for (int i = 0; i < word.text.length(); i++) {
                    grid[word.startY][word.startX + i] = word.text.charAt(i);
                }
            }
        }

        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                System.out.print(grid[i][j] != 0 ? grid[i][j] : '.');
                System.out.print(' ');
            }
            System.out.println();
        }
        System.out.println("---------------------------------------------");
    }
}

/**
 * Individual class
 * Contains list of words and fitness
 */
class Individual {
    public ArrayList<Word> wordsList;
    public int fitness;
    Individual() {
        this.wordsList = new ArrayList<>();
        this.fitness = 100000;
    }
    Individual(ArrayList<Word> initialList) {
        this.wordsList = new ArrayList<>(initialList);
        this.fitness = 100000;
    }
}

/**
 * Word class
 * Contains word text, start coordinates and orientation
 */
class Word {
    String text;
    int startX;
    int startY;
    boolean isVertical;

    Word(String text, int startX, int startY, boolean isVertical) {
        this.text = text;
        this.startX = startX;
        this.startY = startY;
        this.isVertical = isVertical;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Word word = (Word) obj;
        return text.equals(word.text);
    }
    @Override
    public int hashCode() {
        return Objects.hash(text);
    }
}

/**
 * DFS class
 * Contains DFS algorithm
 */
class DFS {
    private final char[][] grid;
    boolean[][] visited;
    int components = 0;

    DFS(char[][] grid) {
        this.grid = grid;
        this.visited = new boolean[20][20];
    }

    void dfs(int x, int y) {
        if (x < 0 || y < 0 || x >= 20 || y >= 20) {
            return;
        }
        if (visited[x][y] || grid[x][y] == 0) {
            return;
        }
        visited[x][y] = true;
        dfs(x - 1, y);
        dfs(x + 1, y);
        dfs(x, y - 1);
        dfs(x, y + 1);
    }

    void startDfs(int x, int y) {
        if (!visited[x][y] && grid[x][y] != 0) {
            dfs(x, y);
            components++;
        }
    }
}
