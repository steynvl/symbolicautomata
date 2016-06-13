package benchmark;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.sat4j.specs.TimeoutException;

import RegexParser.RegexListNode;
import RegexParser.RegexParserProvider;
import automata.safa.SAFA;
import automata.sfa.SFA;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;

public class RunIntersectionExp {
	static FileReader inFile;
	
	public static void main(String[] args) throws TimeoutException {
		ArrayList<String> list = new ArrayList<String>();
		ArrayList<SAFA<CharPred, Character>> safaList = new ArrayList<SAFA<CharPred, Character>>();
		UnaryCharIntervalSolver solver = new UnaryCharIntervalSolver();
		try {
			inFile = new FileReader(args[0]);
		} catch (FileNotFoundException ex) {
			System.err.println("File " + args[0] + " not found.");
			System.exit(-1);
		}
		
		try (BufferedReader br = new BufferedReader(inFile)) {
			String line;
			while ((line = br.readLine()) != null) {
				list.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for(String regex : list){
			safaList.add((new SFAprovider(regex, solver)).getSFA().getSAFA(solver));
		}
		
		System.out.println(hasIntersection(solver, safaList));
		
	}
	
	private static boolean hasIntersection(UnaryCharIntervalSolver solver, ArrayList<SAFA<CharPred, Character>> safaList
			) throws TimeoutException {
		SAFA<CharPred, Character> result;
		if (safaList.size() <2){
			return false;
		}
		
		if (safaList.size() ==2){
			result = safaList.get(0).intersectionWith(safaList.get(1), solver);
			return !SAFA.isEmpty(result,solver);
		}else{
			result = safaList.get(0).intersectionWith(safaList.get(1), solver);
			for (int i=2;i<safaList.size();i++){
				result = result.intersectionWith(safaList.get(i), solver);
			}
			return !SAFA.isEmpty(result,solver);
		}
		
	}

	

}