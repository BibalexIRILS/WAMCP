//   Copyright 2013 Bibliotheca Alexandrina, Wellcome Trust Library
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

package org.bibalex.wamcp.storage;

import java.util.HashMap;
import java.util.Map;

/**
 * A filter that normalizes arabic words to some simpler format.
 * It removes all accents (tashkil) and kashida (tatweel).
 * Also it changes some letters to others.
 * <uol> <li>All types of hamza are changed to alef</li> <li>Zay is changed to Thal</li> <li>Teh marboota is changed to
 * heh marboots</li> <li>Dad is changed to Zah</li> <li>Alef Maksoura (Aleb layyena) is changed to yeh</li> </uol>
 * These rules overcome the most spelling mistakes people do.
 * 
 * @author Ahmed Saad
 * @Author Ahmed Abd-ElHaffiez 2008
 * @author engy.morsy 2011
 */
public class ArabicNormalizer {
	
	private static Map<Character, Character> normalizeMap = getNormalizer();
	
	private static Character getChar(char arGlos) {
		// TODO Auto-generated method stub
		switch (arGlos) {
			case '\u066E':
				return '\u0628';
			case '\u067f':
				return '\u062A';// teh without dot -> teh
			case '\u067D':
				return '\u062B';// theh without dot -> theh
			case '\u06BA':
				return '\u0646';// noon without dot -> noon
			case '\u06A2':
				return '\u0641';// feh one dot above and one dot below -> feh
			case '\u06A1':
				return '\u0641';// feh no dot -> feh
			case '\u069F':
				return '\u0641';// feh dot below and above -> feh
			case '\u066F':
				return '\u0642';// qaf no dot ->qaf
			case '\u0679':
				return '\u064A';// yeh no dot -> yeh
			case '\u067A':
				return '\u064A';// Yeh dots above and below -> yeh
			case '\u06B0':
				return '\u062D';// Hah 2 dots below and above -> hah
			case '\u0678':
				return '\u064A';// Yeh dots below and hamza above -> yeh
			case '\u0622':
				return '\u0627';// alef with madda above is turned into alef
			case '\u0621':
				return '\u0621';// hamza
			case '\u0623':
				return '\u0627';// alef with hamza above is turned into alef
			case '\u0624':
				return '\u0624';// waw with hamza above
			case '\u0625':
				return '\u0627';// alef with hamza below is turned into alef
			case '\u0626':
				return '\u0626';// yeh with hamza above is turned into alef
			case '\u0649':
				return '\u064A';// yeh is turned into alef maksoura
			case '\u064A':
				return '\u064A';// alef maksoura (batta)
			case '\u0671':
				return '\u0627';// alef wasla is turned into alef
				// cancel tashkeel
			case '\u064B':
				return null;
			case '\u064C':
				return null;
			case '\u064D':
				return null;
			case '\u064E':
				return null;
			case '\u064F':
				return null;
			case '\u0650':
				return null;
			case '\u0651':
				return null;
			case '\u0652':
				return null;
			case '\u0653':
				return null;
		}
		
// if ((arGlos >= '\u064B') && (arGlos <= '\u0653')) {
// return null;
// }
		
		return arGlos;
	}
	
	private static Map<Character, Character> getNormalizer() {
		try {
			normalizeMap = new HashMap<Character, Character>();
			// all kinds of alef and hamza
			normalizeMap.put('\u0622', '\u0627');// alef with madda above is turned into alef
			normalizeMap.put('\u0621', '\u0621');// hamza
			normalizeMap.put('\u0623', '\u0627');// alef with hamza above is turned into alef
			normalizeMap.put('\u0624', '\u0624');// waw with hamza above
			normalizeMap.put('\u0625', '\u0627');// alef with hamza below is turned into alef
			normalizeMap.put('\u0626', '\u0626');// yeh with hamza above is turned into alef
			normalizeMap.put('\u0627', '\u0627');// alef
			normalizeMap.put('\u0628', '\u0628');// beh
			normalizeMap.put('\u0629', '\u0629');// teh marboota
			normalizeMap.put('\u062A', '\u062A');// teh
			normalizeMap.put('\u062B', '\u062B');// theh
			normalizeMap.put('\u062C', '\u062C');// jeem
			normalizeMap.put('\u062D', '\u062D');// hah
			normalizeMap.put('\u062E', '\u062E');// khah
			normalizeMap.put('\u062F', '\u062F');// dal
			normalizeMap.put('\u0630', '\u0630');// thal
			normalizeMap.put('\u0631', '\u0631');// reh
			// it's not good to replace zain by zal so i canceled it.
			// normalizeMap.put('\u0632','\u0630');//zay is turned into thal
			normalizeMap.put('\u0632', '\u0632');
			// //////////////////////////////////////////////////////////////
			normalizeMap.put('\u0633', '\u0633');// seen
			normalizeMap.put('\u0634', '\u0634');// sheen
			normalizeMap.put('\u0635', '\u0635');// sad
			normalizeMap.put('\u0636', '\u0636');// dad
			normalizeMap.put('\u0637', '\u0637');// tah
			normalizeMap.put('\u0638', '\u0638');// zah
			normalizeMap.put('\u0639', '\u0639');// ayn
			normalizeMap.put('\u063A', '\u063A');// ghayn
			normalizeMap.put('\u0641', '\u0641');// feh
			normalizeMap.put('\u0642', '\u0642');// qaf
			normalizeMap.put('\u0643', '\u0643');// kaf
			normalizeMap.put('\u0644', '\u0644');// lam
			normalizeMap.put('\u0645', '\u0645');// meem
			normalizeMap.put('\u0646', '\u0646');// noon
			normalizeMap.put('\u0647', '\u0647');// heh
			normalizeMap.put('\u0648', '\u0648');// waw
			normalizeMap.put('\u0649', '\u064A');// yeh is turned into alef maksoura
			normalizeMap.put('\u064A', '\u064A');// alef maksoura (batta)
			normalizeMap.put('\u0671', '\u0627');// alef wasla is turned into alef
			// //////////////////////////////dotless characters for wamcp project
// /////////////////////////////////////////////////////////
			normalizeMap.put('\u066E', '\u0628');// beh without dot - > beh
			normalizeMap.put('\u067f', '\u062A');// teh without dot -> teh
			normalizeMap.put('\u067D', '\u062B');// theh without dot -> theh
			normalizeMap.put('\u06BA', '\u0646');// noon without dot -> noon
			normalizeMap.put('\u06A2', '\u0641');// feh one dot above and one dot below -> feh
			normalizeMap.put('\u06A1', '\u0641');// feh no dot -> feh
			normalizeMap.put('\u069F', '\u0641');// feh dot below and above -> feh
			normalizeMap.put('\u066F', '\u0642');// qaf no dot ->qaf
			normalizeMap.put('\u0679', '\u064A');// yeh no dot -> yeh
			normalizeMap.put('\u067A', '\u064A');// Yeh dots above and below -> yeh
			normalizeMap.put('\u06B0', '\u062D');// Hah 2 dots below and above -> hah
			normalizeMap.put('\u0678', '\u064A');// Yeh dots below and hamza above -> yeh
			// ///////////////////// arabic numbers /////////////////////////////////////////////
			normalizeMap.put('\u0660', '\u0660');// rakam 0
			normalizeMap.put('\u0661', '\u0661');// rakam 1
			normalizeMap.put('\u0662', '\u0662');// rakam 2
			normalizeMap.put('\u0663', '\u0663');// rakam 3
			normalizeMap.put('\u0664', '\u0664');// rakam 4
			normalizeMap.put('\u0665', '\u0665');// rakam 5
			normalizeMap.put('\u0666', '\u0666');// rakam 6
			normalizeMap.put('\u0667', '\u0667');// rakam 7
			normalizeMap.put('\u0668', '\u0668');// rakam 8
			normalizeMap.put('\u0669', '\u0669');// rakam 9
			
			normalizeMap.put('\u0020', '\u0020');
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return normalizeMap;
	}
	
	/**
	 * This function normalizes an Arabic text for most common errors done by people.
	 * This helps indexing it in a normalized form so that they can be searched easily.
	 * 
	 * @param text
	 *            the text to be normalized.
	 */
	public static String normalize(String text) {
		try {
			char[] normalizedText = new char[text.length() * 2];
			int i = 0;
			char[] arGloss = text.toCharArray();
			
			for (char arGlos : arGloss) {
				Character result = getChar(arGlos);
				if (result != null) {
					normalizedText[i++] = result.charValue();
				}
			}
			return i != 0 ? new String(normalizedText, 0, i) : null;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
}
