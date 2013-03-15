package spellcheck;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple spell checker. The program reads a file into memory, asks the user to enter a word
 * to find, and returns either the word, a best match, or "Couldn't find your word!" 
 * if it could not be found.
 * 
 * The file being read in can be specified as a command line argument. Otherwise, the user
 * can enter the file location/name, such as "english.dict" (file provided in project).
 * 
 * @author Eric Jackson
 * 
 */
public class SpellCheck {

	/** String for checking proper user input */
	private static final String PATTERN_LETTERS_ONLY = "[a-zA-Z]*";
	
	/** String to match all vowels, including 'y' */
	private static final String PATTERN_VOWELS = "[AaEeIiOoUuYy]";

	/** Integer to indicate no match was found when searching for the word */
	private static final int NO_MATCH = -1;
	
	/** Map of each word and the word without vowels */
	private Map<String, String> dictionary = null;
	
	/** The resulting string that was found in the dictionary */
	private String result = "";
	
	/** The collection of possible results */
	private List<String> results = null;
	

	/**
	 * Constructor.
	 * Reads the file into memory and starts the word checking loop.
	 * 
	 * @param fileName the name/location of the file being read in
	 */
	SpellCheck(String fileName) {
		// Create a new File based on the file name
		File file = new File(fileName);
		
		// Read the file into memory
		if (!readFile(file)) {
			System.err.println("Error reading file, cannot continue.");
			System.exit(1);
		}
		
		// Create reader for user input
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		// Start asking the user for words
		System.out.println("What are you looking for today?");
		String word = "";
		while (true) {
			System.out.print("> ");
			
			// Search for the word
			try {
				word = br.readLine().trim();
				System.out.println(checkWord(word));
			} catch (IOException e) {
				System.err.println("Error while checking for your word:");
				e.printStackTrace();
				System.err.println("Program must exit.");
				System.exit(1);
			} catch (NullPointerException e) {
				System.err.println("Exiting program.");
				System.exit(1);
			}
		}
	}
	
	/**
	 * Read the contents of a file into a data structure.
	 * 
	 * @param file the File being read in
	 * @return true if there were no errors while reading the file
	 */
	private boolean readFile(File file) {
		// Create a new holder for storing words and the words without vowels
		dictionary = new LinkedHashMap<String, String>();
		
		// Create possible results list
		results = new ArrayList<String>();
		
		// Read the file into memory
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			
			String word = "";
			while ((word = br.readLine()) != null) {
				// Storing both the regular and no vowel word
				dictionary.put(word, removeVowels(word));
			}
			
			System.out.println("Dictionary has " + dictionary.size() + " words.");
			
			// Close reader
			br.close();
			
		} catch (IOException e) {
			System.err.println("Error reading file:");
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Check the dictionary for the specified word.
	 * @param word the word to look for
	 * @return the found or suggested word, or "Couldn't find your word!" if nothing is found
	 */
	private String checkWord(String word) {
		// Reset results list
		results = new ArrayList<String>();
		
		// Check for proper input: letters only
		if (!word.matches(PATTERN_LETTERS_ONLY)) {
			return "Word must contain only letters.";
		}
		
		// Check for proper input: can't do single letters
		if (word.length() <= 1) {
			return "Word must be longer than one letter.";
		}
		
		// Check if the word is already in the dictionary, regardless of uppercase/lowercase letters
		if (isInDictionary(word)) {
			System.out.println("Found " + word + " in dictionary: " + result);
			return result;
		}
		
		// If not found still, check Levenshtein distance
		if (checkDistance(word)) {
			System.out.println("Found " + word + " in dictionary: " + result);
			return result;
		}
		
		// If not found still, check for incorrect letter placement
		if (checkLetterMatches(word)) {
			System.out.println("Found " + word + " in dictionary: " + result);
			return result;
		}
		
		// If not found still, check for incorrect vowels
		if (checkVowels(word)) {
			System.out.println("Found " + word + " in dictionary: " + result);
			return result;
		}
		
		// If still not found, doesn't exist
		return "Couldn't find your word!";
	}
	
	/**
	 * Checks the dictionary for the word.
	 * @param word the word to search for
	 * @return true if the word is found, false otherwise
	 */
	private boolean isInDictionary(String word) {
		// See if it exists right away
		word = word.toLowerCase();
		if (dictionary.containsKey(word)) {
			result = word;
			return true;
		}
		
		// Didn't find it
		return false;
	}

	/**
	 * Checks the Levenshtein distance between the word and all words in the dictionary. Then,
	 * it checks the number of matching letters and returns a result based on that check.
	 * @param word the word to be found
	 * @return true if we found some possible results, false otherwise
	 */
	private boolean checkDistance(String word) {
		LevenshteinDistance ld = new LevenshteinDistance();
		
		// Setting our minimum to the max initially
		int min = Integer.MAX_VALUE;
		
		// Check each string in the dictionary
		for (String s : dictionary.keySet()) {
			int distance = ld.computeLevenshteinDistance(s, word);
			
			// See if we have a new minimum distance
			if (distance < min) {
				min = distance;
				result = s;
			}
		}
		
		// Check the amount of matching letters and see if we get a result based on this
		return checkPossibleResults(result);
	}

	/**
	 * Check to see if the word has any misplaced letters.
	 * @param word the word to be found
	 * @return true if we found some possible results, false otherwise
	 */
	private boolean checkLetterMatches(String word) {
		// Convert to lowercase
		word = word.toLowerCase();
		
		// Checking lengths to add possible matches
		for (String s : dictionary.keySet()) {
			if (s.length() == word.length()) {
				results.add(s);
			}
		}
		
		return checkPossibleResults(word);
	}

	/**
	 * Check to see if the word has any misplaced vowels.
	 * @param word the word to be found
	 * @return true if we found some possible results, false otherwise
	 */
	private boolean checkVowels(String word) {
		// Convert to lowercase
		word = word.toLowerCase();
		
		// Remove vowels from user's word
		String noVowels = removeVowels(word);
		
		// Check for same length as original word with vowels
		for (String s : dictionary.keySet()) {
			
			// Get the "no vowel" value of the regular word key and compare
			if (dictionary.get(s).equals(noVowels) && (s.length() == word.length())) {				
				results.add(s);
			}
		}
		
		return checkPossibleResults(word);
	}

	/**
	 * Checks for best possible matches between the word and the list of possible results.
	 * @param word the word to be found in the dictionary
	 * @return true if a best match was found, false otherwise
	 */
	private boolean checkPossibleResults(String word) {
		// Get all possible results
		if (results.size() > 0) {
			
			// Find which word has the largest number of matching letters
			int indexOfHighestMatch = countMatchingLetters(word);
			
			// If we didn't match at least 50%, don't count it
			if (indexOfHighestMatch == NO_MATCH) {
				return false;
			}
			
			// Choose first word in results list
			result = results.get(indexOfHighestMatch);
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Counts the number of matching letters between the list of suggested words
	 * and the word trying to be found.
	 * 
	 * If the number of matching letters is less than half the length of the word (in
	 * other words, more than half the word is misspelled), don't count it as a
	 * suggestion. Otherwise many gibberish words from the user will return a
	 * real word.
	 * 
	 * @param word the word trying to be found
	 * @return the index of of the word in the suggested words list with the highest
	 * number of matching letters
	 */
	private int countMatchingLetters(String word) {
		// Initialize best match counting list
		List<Integer> counts = new ArrayList<Integer>(results.size());
		for (int i = 0; i < results.size(); i++) {
			counts.add(0);
		}
		
		// By counting the number of matching characters, we can find the best suggestion(s)
		for (String s : results) {
			int numMatches = 0;
			for (int i = 0; i < word.length(); i++) {
				if (word.charAt(i) == s.charAt(i)) {
					numMatches++;
					counts.set(results.indexOf(s), numMatches);
				}
			}
		}
		
		// Get the first index with highest number of matching letters
		int max = Collections.max(counts);
		
		System.out.println("Number of matching letters: " + max);
		
		// Check for at least 50% matching, we don't want poor matches
		if (max  < word.length() / 2) {
			return NO_MATCH;
		}
		
		for (Integer i : counts) {
			if (i == max) {
				return counts.indexOf(max);
			}
		}
		
		// Default to index 0 if we somehow didn't match any indices above
		return NO_MATCH;
	}

	/**
	 * Removes all vowels from a word (including 'y').
	 * @param word the word to remove vowels from
	 * @return the vowel-free word
	 */
	private String removeVowels(String word) {
		return word.replaceAll(PATTERN_VOWELS, "");
	}

	/**
	 * Start the program.
	 * @param args arguments needed by the program. In this case, the file location
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// The file name/location
		String fileName = "";
		
		// If we don't have a file specified, ask for it
		if (args.length == 0) {
			while (true) {
				// Create reader for reading user input
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				
				System.out.print("Please specify the file location/name:" + System.lineSeparator() + "> ");
				
				// Read what the user enters
				try {
					fileName = br.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				// Check if the file exists
				File file = new File(fileName);
				if (file.exists()) {
					break;
				}
				else {
					System.err.println("Invalid file location/name: " + fileName);
				}
			}
		}
		// Otherwise, use the file specified
		else {
			fileName = args[0];
		}
		
		// Start the program
		new SpellCheck(fileName);
	}
	
	
	/**
	 * Helper class to compute the Levenshtein distance between two words.
	 * @author Wikipedia page for Levenshtein distance!
	 */
	private class LevenshteinDistance {
		/**
		 * Return the minimum between three variables.
		 * @param a
		 * @param b
		 * @param c
		 * @return the minimum between a and b, and then the minimum between that and c
		 */
        private int minimum(int a, int b, int c) {
                return Math.min(Math.min(a, b), c);
        }
 
        /**
         * Compute the Levenshtein distance between two words.
         * @param word1 the first word being compared
         * @param word2 the second word being compared to the first
         * @return the Levenshtein distance between the two words
         */
        public int computeLevenshteinDistance(CharSequence word1, CharSequence word2) {
                int[][] distance = new int[word1.length() + 1][word2.length() + 1];
 
                for (int i = 0; i <= word1.length(); i++)
                        distance[i][0] = i;
                for (int j = 1; j <= word2.length(); j++)
                        distance[0][j] = j;
 
                for (int i = 1; i <= word1.length(); i++)
                        for (int j = 1; j <= word2.length(); j++)
                                distance[i][j] = minimum(
                                                distance[i - 1][j] + 1,
                                                distance[i][j - 1] + 1,
                                                distance[i - 1][j - 1] +
                                                ((word1.charAt(i - 1) == word2.charAt(j - 1)) ? 0 : 1));
 
                return distance[word1.length()][word2.length()];
        }
	}
}
