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

package org.bibalex.gallery.storage;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

// Copyright (C) 2008 Alain COUTHURES
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

class light_html2xml {
	enum states {
		text, tag, endtag, attrtext, script, endscript, specialtag, comment, skipcdata, entity, namedentity, numericentity, hexaentity, tillgt, tillquote, tillinst, andgt
	};
	
	private static HashMap<String, Integer> namedentities = new HashMap<String, Integer>();
	private static List<String> emptytags = new ArrayList<String>();
	private static HashMap<String, List<String>> autoclosetags = new HashMap<String, List<String>>();
	
	public static String Html2Xml(String s) {
		namedentities.put("AElig", 198);
		namedentities.put("Aacute", 193);
		namedentities.put("Acirc", 194);
		namedentities.put("Agrave", 192);
		namedentities.put("Alpha", 913);
		namedentities.put("Aring", 197);
		namedentities.put("Atilde", 195);
		namedentities.put("Auml", 196);
		namedentities.put("Beta", 914);
		namedentities.put("Ccedil", 199);
		namedentities.put("Chi", 935);
		namedentities.put("Dagger", 8225);
		namedentities.put("Delta", 916);
		namedentities.put("ETH", 208);
		namedentities.put("Eacute", 201);
		namedentities.put("Ecirc", 202);
		namedentities.put("Egrave", 200);
		namedentities.put("Epsilon", 917);
		namedentities.put("Eta", 919);
		namedentities.put("Euml", 203);
		namedentities.put("Gamma", 915);
		namedentities.put("Iacute", 205);
		namedentities.put("Icirc", 206);
		namedentities.put("Igrave", 204);
		namedentities.put("Iota", 921);
		namedentities.put("Iuml", 207);
		namedentities.put("Kappa", 922);
		namedentities.put("Lambda", 923);
		namedentities.put("Mu", 924);
		namedentities.put("Ntilde", 209);
		namedentities.put("Nu", 925);
		namedentities.put("OElig", 338);
		namedentities.put("Oacute", 211);
		namedentities.put("Ocirc", 212);
		namedentities.put("Ograve", 210);
		namedentities.put("Omega", 937);
		namedentities.put("Omicron", 927);
		namedentities.put("Oslash", 216);
		namedentities.put("Otilde", 213);
		namedentities.put("Ouml", 214);
		namedentities.put("Phi", 934);
		namedentities.put("Pi", 928);
		namedentities.put("Prime", 8243);
		namedentities.put("Psi", 936);
		namedentities.put("Rho", 929);
		namedentities.put("Scaron", 352);
		namedentities.put("Sigma", 931);
		namedentities.put("THORN", 222);
		namedentities.put("Tau", 932);
		namedentities.put("Theta", 920);
		namedentities.put("Uacute", 218);
		namedentities.put("Ucirc", 219);
		namedentities.put("Ugrave", 217);
		namedentities.put("Upsilon", 933);
		namedentities.put("Uuml", 220);
		namedentities.put("Xi", 926);
		namedentities.put("Yacute", 221);
		namedentities.put("Yuml", 376);
		namedentities.put("Zeta", 918);
		namedentities.put("aacute", 225);
		namedentities.put("acirc", 226);
		namedentities.put("acute", 180);
		namedentities.put("aelig", 230);
		namedentities.put("agrave", 224);
		namedentities.put("alpha", 945);
		namedentities.put("and", 8743);
		namedentities.put("ang", 8736);
		namedentities.put("aring", 229);
		namedentities.put("asymp", 8776);
		namedentities.put("atilde", 227);
		namedentities.put("auml", 228);
		namedentities.put("bdquo", 8222);
		namedentities.put("beta", 946);
		namedentities.put("brvbar", 166);
		namedentities.put("bull", 8226);
		namedentities.put("cap", 8745);
		namedentities.put("ccedil", 231);
		namedentities.put("cedil", 184);
		namedentities.put("cent", 162);
		namedentities.put("chi", 967);
		namedentities.put("circ", 710);
		namedentities.put("clubs", 9827);
		namedentities.put("cong", 8773);
		namedentities.put("copy", 169);
		namedentities.put("crarr", 8629);
		namedentities.put("cup", 8746);
		namedentities.put("curren", 164);
		namedentities.put("dagger", 8224);
		namedentities.put("darr", 8595);
		namedentities.put("deg", 176);
		namedentities.put("delta", 948);
		namedentities.put("diams", 9830);
		namedentities.put("divide", 247);
		namedentities.put("eacute", 233);
		namedentities.put("ecirc", 234);
		namedentities.put("egrave", 232);
		namedentities.put("empty", 8709);
		namedentities.put("emsp", 8195);
		namedentities.put("ensp", 8194);
		namedentities.put("epsilon", 949);
		namedentities.put("equiv", 8801);
		namedentities.put("eta", 951);
		namedentities.put("eth", 240);
		namedentities.put("euml", 235);
		namedentities.put("euro", 8364);
		namedentities.put("exists", 8707);
		namedentities.put("fnof", 402);
		namedentities.put("forall", 8704);
		namedentities.put("frac12", 189);
		namedentities.put("frac14", 188);
		namedentities.put("frac34", 190);
		namedentities.put("gamma", 947);
		namedentities.put("ge", 8805);
		namedentities.put("harr", 8596);
		namedentities.put("hearts", 9829);
		namedentities.put("hellip", 8230);
		namedentities.put("iacute", 237);
		namedentities.put("icirc", 238);
		namedentities.put("iexcl", 161);
		namedentities.put("igrave", 236);
		namedentities.put("infin", 8734);
		namedentities.put("int", 8747);
		namedentities.put("iota", 953);
		namedentities.put("iquest", 191);
		namedentities.put("isin", 8712);
		namedentities.put("iuml", 239);
		namedentities.put("kappa", 954);
		namedentities.put("lambda", 923);
		namedentities.put("laquo", 171);
		namedentities.put("larr", 8592);
		namedentities.put("lceil", 8968);
		namedentities.put("ldquo", 8220);
		namedentities.put("le", 8804);
		namedentities.put("lfloor", 8970);
		namedentities.put("lowast", 8727);
		namedentities.put("loz", 9674);
		namedentities.put("lrm", 8206);
		namedentities.put("lsaquo", 8249);
		namedentities.put("lsquo", 8216);
		namedentities.put("macr", 175);
		namedentities.put("mdash", 8212);
		namedentities.put("micro", 181);
		namedentities.put("middot", 183);
		namedentities.put("minus", 8722);
		namedentities.put("mu", 956);
		namedentities.put("nabla", 8711);
		namedentities.put("nbsp", 160);
		namedentities.put("ndash", 8211);
		namedentities.put("ne", 8800);
		namedentities.put("ni", 8715);
		namedentities.put("not", 172);
		namedentities.put("notin", 8713);
		namedentities.put("nsub", 8836);
		namedentities.put("ntilde", 241);
		namedentities.put("nu", 925);
		namedentities.put("oacute", 243);
		namedentities.put("ocirc", 244);
		namedentities.put("oelig", 339);
		namedentities.put("ograve", 242);
		namedentities.put("oline", 8254);
		namedentities.put("omega", 969);
		namedentities.put("omicron", 959);
		namedentities.put("oplus", 8853);
		namedentities.put("or", 8744);
		namedentities.put("ordf", 170);
		namedentities.put("ordm", 186);
		namedentities.put("oslash", 248);
		namedentities.put("otilde", 245);
		namedentities.put("otimes", 8855);
		namedentities.put("ouml", 246);
		namedentities.put("para", 182);
		namedentities.put("part", 8706);
		namedentities.put("permil", 8240);
		namedentities.put("perp", 8869);
		namedentities.put("phi", 966);
		namedentities.put("pi", 960);
		namedentities.put("piv", 982);
		namedentities.put("plusmn", 177);
		namedentities.put("pound", 163);
		namedentities.put("prime", 8242);
		namedentities.put("prod", 8719);
		namedentities.put("prop", 8733);
		namedentities.put("psi", 968);
		namedentities.put("radic", 8730);
		namedentities.put("raquo", 187);
		namedentities.put("rarr", 8594);
		namedentities.put("rceil", 8969);
		namedentities.put("rdquo", 8221);
		namedentities.put("reg", 174);
		namedentities.put("rfloor", 8971);
		namedentities.put("rho", 961);
		namedentities.put("rlm", 8207);
		namedentities.put("rsaquo", 8250);
		namedentities.put("rsquo", 8217);
		namedentities.put("sbquo", 8218);
		namedentities.put("scaron", 353);
		namedentities.put("sdot", 8901);
		namedentities.put("sect", 167);
		namedentities.put("shy", 173);
		namedentities.put("sigma", 963);
		namedentities.put("sigmaf", 962);
		namedentities.put("sim", 8764);
		namedentities.put("spades", 9824);
		namedentities.put("sub", 8834);
		namedentities.put("sube", 8838);
		namedentities.put("sum", 8721);
		namedentities.put("sup", 8835);
		namedentities.put("sup1", 185);
		namedentities.put("sup3", 179);
		namedentities.put("supe", 8839);
		namedentities.put("szlig", 223);
		namedentities.put("tau", 964);
		namedentities.put("there4", 8756);
		namedentities.put("theta", 952);
		namedentities.put("thetasym", 977);
		namedentities.put("thinsp", 8201);
		namedentities.put("thorn", 254);
		namedentities.put("tilde", 732);
		namedentities.put("times", 215);
		namedentities.put("trade", 8482);
		namedentities.put("uacute", 250);
		namedentities.put("uarr", 8593);
		namedentities.put("ucirc", 251);
		namedentities.put("ugrave", 249);
		namedentities.put("uml", 168);
		namedentities.put("up2", 178);
		namedentities.put("upsih", 978);
		namedentities.put("upsilon", 965);
		namedentities.put("uuml", 252);
		namedentities.put("xi", 958);
		namedentities.put("yacute", 253);
		namedentities.put("yen", 165);
		namedentities.put("yuml", 255);
		namedentities.put("zeta", 950);
		namedentities.put("zwj", 8205);
		namedentities.put("zwnj", 8204);
		emptytags.add("area");
		emptytags.add("base");
		emptytags.add("basefont");
		emptytags.add("br");
		emptytags.add("col");
		emptytags.add("frame");
		emptytags.add("hr");
		emptytags.add("img");
		emptytags.add("input");
		emptytags.add("isindex");
		emptytags.add("link");
		emptytags.add("meta");
		emptytags.add("param");
		autoclosetags.put("basefont", new ArrayList<String>());
		autoclosetags.get("basefont").add("basefont");
		autoclosetags.put("colgroup", new ArrayList<String>());
		autoclosetags.get("colgroup").add("colgroup");
		autoclosetags.put("dd", new ArrayList<String>());
		autoclosetags.get("dd").add("colgroup");
		autoclosetags.put("dt", new ArrayList<String>());
		autoclosetags.get("dt").add("dt");
		autoclosetags.put("li", new ArrayList<String>());
		autoclosetags.get("li").add("li");
		autoclosetags.put("p", new ArrayList<String>());
		autoclosetags.get("p").add("p");
		autoclosetags.put("thead", new ArrayList<String>());
		autoclosetags.get("thead").add("tbody");
		autoclosetags.get("thead").add("tfoot");
		autoclosetags.put("tbody", new ArrayList<String>());
		autoclosetags.get("tbody").add("thead");
		autoclosetags.get("tbody").add("tfoot");
		autoclosetags.put("tfoot", new ArrayList<String>());
		autoclosetags.get("tfoot").add("thead");
		autoclosetags.get("tfoot").add("tbody");
		autoclosetags.put("th", new ArrayList<String>());
		autoclosetags.get("th").add("td");
		autoclosetags.put("td", new ArrayList<String>());
		autoclosetags.get("td").add("th");
		autoclosetags.get("td").add("td");
		autoclosetags.put("tr", new ArrayList<String>());
		autoclosetags.get("tr").add("tr");
		String r2 = "";
		String r = "";
		int limit = s.length();
		states state = states.text;
		states prevstate = state;
		Stack<String> opentags = new Stack<String>();
		String name = "";
		String tagname = "";
		String attrname = "";
		String attrs = "";
		List<String> attrnames = new ArrayList<String>();
		int entvalue = 0;
		char attrdelim = '"';
		String attrvalue = "";
		String cs = "";
		char prec = ' ';
		char preprec = ' ';
		char c = ' ';
		int start = 0;
		String encoding = "";
		if ((s.charAt(0) == 0xEF) && (s.charAt(1) == 0xBB) && (s.charAt(2) == 0xBF)) {
			encoding = "utf-8";
			start = 3;
		} else {
			encoding = "iso-8859-1";
			start = 0;
		}
		for (int i = start; (i < limit) && ((r2.equals("") && r.equals("")) || !opentags.empty()); i++) {
			if (r.length() > 10240) {
				r2 += r;
				r = "";
			}
			c = s.charAt(i);
			switch (state) {
				case text:
					if (c == '<') {
						name = "";
						tagname = "";
						attrname = "";
						attrs = "";
						attrnames.clear();
						state = states.tag;
						break;
					}
					if (!Character.isWhitespace(c) && opentags.empty()) {
						r += "<html>";
						opentags.push("html");
					}
					if (Character.isWhitespace(c) && opentags.empty()) {
						break;
					}
					if (c == '&') {
						name = "";
						entvalue = 0;
						prevstate = state;
						state = states.entity;
						break;
					}
					r += c;
					break;
				case tag:
					if ((c == '?') && tagname.equals("")) {
						state = states.tillinst;
						break;
					}
					if ((c == '!') && tagname.equals("")) {
						state = states.specialtag;
						prec = ' ';
						break;
					}
					if ((c == '/') && name.equals("") && tagname.equals("")) {
						state = states.endtag;
						name = "";
						break;
					}
					if (Character.isWhitespace(c)) {
						if (name.equals("")) {
							break;
						}
						if (tagname.equals("") && (name != "_")) {
							tagname = name;
							name = "";
							break;
						}
						if (attrname.equals("")) {
							attrname = name.toLowerCase();
							name = "";
							break;
						}
						break;
					}
					if (c == '=') {
						if (attrname.equals("")) {
							attrname = name.toLowerCase();
							name = "";
						}
						state = states.tillquote;
						break;
					}
					if ((c == '/') && (!tagname.equals("") || !name.equals(""))) {
						if (tagname.equals("")) {
							tagname = name;
						}
						tagname = tagname.toLowerCase();
						if (!tagname.equals("html") && opentags.empty()) {
							r += "<html>";
							opentags.push("html");
						}
						if (autoclosetags.containsKey(tagname) && !opentags.empty()) {
							String prevtag = opentags.peek();
							if (autoclosetags.get(tagname).contains(prevtag)) {
								opentags.pop();
								r += "</" + prevtag + ">";
							}
						}
						if (tagname.equals("tr") && opentags.peek().equals("table")) {
							r += "<tbody>";
							opentags.push("tbody");
						}
						r += "<" + tagname + attrs + "/>";
						state = states.tillgt;
						break;
					}
					if (c == '>') {
						if (tagname.equals("") && !name.equals("")) {
							tagname = name;
						}
						if (!tagname.equals("")) {
							tagname = tagname.toLowerCase();
							if (!tagname.equals("html") && opentags.empty()) {
								r += "<html>";
								opentags.push("html");
							}
							if (autoclosetags.containsKey(tagname) && !opentags.empty()) {
								String prevtag = opentags.peek();
								if (autoclosetags.get(tagname).contains(prevtag)) {
									opentags.pop();
									r += "</" + prevtag + ">";
								}
							}
							if (tagname.equals("tr") && opentags.peek().equals("table")) {
								r += "<tbody>";
								opentags.push("tbody");
							}
							if (emptytags.contains(tagname)) {
								r += "<" + tagname.toLowerCase() + attrs + "/>";
							} else {
								opentags.push(tagname);
								r += "<" + tagname + attrs + ">";
								if (tagname.equals("script")) {
									r += "<![CDATA[";
									opentags.pop();
									state = states.script;
									break;
								}
							}
							state = states.text;
							break;
						}
					}
					if (attrname.equals("_")) {
						while (attrnames.contains(attrname)) {
							attrname += "_";
						}
					}
					if (!attrname.equals("") && !attrnames.contains(attrname)
							&& !attrname.equals("xmlns")) {
						attrs += " " + attrname + "=\"" + attrname + "\"";
						attrname = "";
					}
					cs = "" + c;
					name += (Character.isLetterOrDigit(c) && (name != "")) || Character.isLetter(c) ? cs
							: (name.equals("") ? "_" : (c == '-' ? "-" : (!name.equals("_") ? "_"
									: "")));
					break;
				case endtag:
					if (c == '>') {
						name = name.toLowerCase();
						if (opentags.search(name) != -1) {
							String prevtag;
							while (!(prevtag = opentags.pop()).equals(name)) {
								r += "</" + prevtag + ">";
							}
							r += "</" + name + ">";
						} else {
							if (!name.equals("html") && opentags.empty()) {
								r += "<html>";
								opentags.push("html");
							}
						}
						state = states.text;
						break;
					}
					if (Character.isWhitespace(c)) {
						break;
					}
					cs = "" + c;
					name += Character.isLetterOrDigit(c) ? cs : !name.equals("_") ? "_" : "";
					break;
				case attrtext:
					if ((c == attrdelim) || (Character.isWhitespace(c) && (attrdelim == ' '))) {
						if (attrname.equals("_")) {
							while (attrnames.contains(attrname)) {
								attrname += "_";
							}
						}
						if (!attrnames.contains(attrname) && !attrname.equals("xmlns")) {
							attrnames.add(attrname);
							attrs += " " + attrname + "=\"" + attrvalue + "\"";
						}
						attrname = "";
						state = states.tag;
						break;
					}
					if ((attrdelim == ' ') && ((c == '/') || (c == '>'))) {
						tagname = tagname.toLowerCase();
						if (!tagname.equals("html") && opentags.empty()) {
							r += "<html>";
							opentags.push("html");
						}
						if (autoclosetags.containsKey(tagname) && !opentags.empty()) {
							String prevtag = opentags.peek();
							if (autoclosetags.get(tagname).contains(prevtag)) {
								opentags.pop();
								r += "</" + prevtag + ">";
							}
						}
						if (attrname.equals("_")) {
							while (attrnames.contains(attrname)) {
								attrname += "_";
							}
						}
						if (!attrnames.contains(attrname) && !attrname.equals("xmlns")) {
							attrnames.add(attrname);
							attrs += " " + attrname + "=\"" + attrvalue + "\"";
						}
						attrname = "";
						if (c == '/') {
							r += "<" + tagname + attrs + "/>";
							state = states.tillgt;
							break;
						}
						if (c == '>') {
							if (emptytags.contains(tagname)) {
								r += "<" + tagname + attrs + "/>";
								state = states.text;
								break;
							} else {
								opentags.push(tagname);
								r += "<" + tagname + attrs + ">";
								if (tagname.equals("script")) {
									r += "<![CDATA[";
									opentags.pop();
									prec = ' ';
									preprec = ' ';
									state = states.script;
									break;
								}
								state = states.text;
								break;
							}
						}
					}
					if (c == '&') {
						name = "";
						entvalue = 0;
						prevstate = state;
						state = states.entity;
						break;
					}
					cs = "" + c;
					attrvalue += c == '"' ? "&quot;" : c == '\'' ? "&apos;" : cs;
					break;
				case script:
					if ((c == '/') && (prec == '<')) {
						state = states.endscript;
						name = "";
						break;
					}
					if ((c == '[') && (prec == '!') && (preprec == '<')) {
						state = states.skipcdata;
						name = "<![";
						break;
					}
					if ((c == '>') && (prec == ']') && (preprec == ']')) {
						c = r.charAt(r.length() - 3);
						r = r.substring(0, r.length() - 4);
					}
					r += c;
					preprec = prec;
					prec = c;
					break;
				case endscript:
					if ((c == '>') && name.toLowerCase().equals("script")) {
						r = r.substring(0, r.length() - 1);
						r += "]]></script>";
						state = states.text;
						break;
					}
					name += c;
					String sscr = "script";
					if (!sscr.startsWith(name.toLowerCase())) {
						r += name;
						state = states.script;
					}
					break;
				case specialtag:
					if (c != '-') {
						state = states.tillgt;
						break;
					}
					if (prec == '-') {
						state = states.comment;
						preprec = ' ';
						break;
					}
					prec = c;
					break;
				case comment:
					if ((c == '>') && (prec == '-') && (preprec == '-')) {
						state = states.text;
						break;
					}
					preprec = prec;
					prec = c;
					break;
				case skipcdata:
					if (name.equals("<![CDATA[")) {
						state = states.script;
						break;
					}
					name += c;
					String scdata = "<![CDATA[";
					if (!scdata.startsWith(name)) {
						r += name;
						state = states.script;
					}
					break;
				case entity:
					if (c == '#') {
						state = states.numericentity;
						break;
					}
					name += c;
					state = states.namedentity;
					break;
				case numericentity:
					if ((c == 'x') || (c == 'X')) {
						state = states.hexaentity;
						break;
					}
					if (c == ';') {
						String ent = "&#" + entvalue + ";";
						if (prevstate == states.text) {
							r += ent;
						} else {
							attrvalue += ent;
						}
						state = prevstate;
						break;
					}
					entvalue = entvalue * 10 + c - '0';
					break;
				case hexaentity:
					if (c == ';') {
						String ent = "&#" + entvalue + ";";
						if (prevstate == states.text) {
							r += ent;
						} else {
							attrvalue += ent;
						}
						state = prevstate;
						break;
					}
					entvalue = entvalue * 16
							+ (Character.isDigit(c) ? c - '0' : Character.toUpperCase(c) - 'A');
					break;
				case namedentity:
					if (c == ';') {
						String ent;
						name = name.toLowerCase();
						if (name.equals("amp") || name.equals("lt") || name.equals("gt")
								|| name.equals("quot") || name.equals("apos")) {
							ent = "&" + name + ";";
							name = "";
							if (prevstate == states.text) {
								r += ent;
							} else {
								attrvalue += ent;
							}
							state = prevstate;
							break;
						}
						if (namedentities.containsKey(name)) {
							entvalue = namedentities.get(name);
						} else {
							entvalue = 0;
						}
						ent = "&#" + entvalue + ";";
						name = "";
						if (prevstate == states.text) {
							r += ent;
						} else {
							attrvalue += ent;
						}
						state = prevstate;
						break;
					}
					if (!Character.isLetterOrDigit(c) || (name.length() > 6)) {
						String ent = "&amp;" + name;
						name = "";
						if (prevstate == states.text) {
							r += ent;
						} else {
							attrvalue += ent;
						}
						state = prevstate;
						i--;
						break;
					}
					name += c;
					break;
				case tillinst:
					if (c == '?') {
						state = states.andgt;
					}
					break;
				case andgt:
					if (c == '>') {
						state = states.text;
						break;
					}
					state = states.tillinst;
					break;
				case tillgt:
					if (c == '>') {
						state = states.text;
					}
					break;
				case tillquote:
					if (Character.isWhitespace(c)) {
						break;
					}
					if ((c == '"') || (c == '\'')) {
						attrdelim = c;
						attrvalue = "";
						state = states.attrtext;
						break;
					}
					if ((c == '/') || (c == '>')) {
						if (attrname.equals("_")) {
							while (attrnames.contains(attrname)) {
								attrname += "_";
							}
						}
						if (!attrnames.contains(attrname) && !attrname.equals("xmlns")) {
							attrnames.add(attrname);
							attrs += " " + attrname + "=\"" + attrvalue + "\"";
						}
						attrname = "";
					}
					if (c == '/') {
						r += "<" + tagname.toLowerCase() + attrs + "/>";
						state = states.tillgt;
						break;
					}
					if (c == '>') {
						tagname = tagname.toLowerCase();
						if (!tagname.equals("html") && opentags.empty()) {
							r += "<html>";
							opentags.push("html");
						}
						if (autoclosetags.containsKey(tagname) && !opentags.empty()) {
							String prevtag = opentags.peek();
							if (autoclosetags.get(tagname).contains(prevtag)) {
								opentags.pop();
								r += "</" + prevtag + ">";
							}
						}
						if (emptytags.contains(tagname)) {
							r += "<" + tagname + attrs + "/>";
							state = states.text;
							break;
						} else {
							opentags.push(tagname);
							r += "<" + tagname + attrs + ">";
							if (tagname.equals("script")) {
								r += "<![CDATA[";
								opentags.pop();
								state = states.script;
								break;
							}
						}
					}
					attrdelim = ' ';
					attrvalue = "" + c;
					state = states.attrtext;
					break;
			}
		}
		while (!opentags.empty()) {
			r += "</" + opentags.pop() + ">";
		}
		r2 += r;
		return "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n" + r2;
	}
	
	public static void main(String[] args) {
		try {
			String fn = "w3";
			FileReader fr = new FileReader(fn + ".htm");
			BufferedReader br = new BufferedReader(fr);
			String s = "";
			while (br.ready()) {
				s += br.readLine() + "\n";
			}
			br.close();
			fr.close();
			FileWriter fw = new FileWriter(fn + ".xml");
			PrintWriter pw = new PrintWriter(fw);
			pw.print(Html2Xml(s));
			pw.close();
		} catch (FileNotFoundException e) {
			System.out.println("file not found");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}