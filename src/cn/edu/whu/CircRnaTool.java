package cn.edu.whu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;
import java.util.Vector;

import cn.edu.whu.exception.FileReadException;
import cn.edu.whu.util.Constant;

public class CircRnaTool implements Serializable {
	private static final long serialVersionUID = 1L;

	public CircRnaTool() {
	}

	public static boolean initSpeciesDataFromFile(File file, TreeMap<String, Gene> genes) {
		return parseGeneTranscriptFile(file, genes);
	}

	public static boolean initCircRnaDataFromFiles(Vector<Vector<String>> fileTableData, String speciesName,
			TreeMap<String, Gene> genes) throws FileReadException {
		return addCircRnaData(fileTableData, speciesName, genes);
	}

	private static boolean addCircRnaData(Vector<Vector<String>> fileTableData, String speciesName,
			TreeMap<String, Gene> genes) throws FileReadException {
		TreeMap<String, CircRna> circRnas = new TreeMap<String, CircRna>();
		for (Vector<String> rowData : fileTableData) {
			String species = rowData.get(0);
			if (!species.equalsIgnoreCase(speciesName)) {
				continue;
			}
			String toolName = rowData.get(1);
			String fileName = rowData.get(2);
			String filePath = rowData.get(3);
			String[] tmp = fileName.split("\\.");
			String sampleName = tmp[0];
			File file = new File(filePath);
			if (toolName.equalsIgnoreCase(Constant.TOOL_CIRCRNAFINDER)
					|| toolName.equalsIgnoreCase(Constant.TOOL_FIND_CIRC)) {
				if (!parseCircRnaFinderFile(file, circRnas, sampleName, toolName, fileName)) {
					throw new FileReadException("Can not open [" + file.getName() + "] or file FORMAT ERROR!");
				}
			} else if (toolName.equalsIgnoreCase(Constant.TOOL_CIRI)) {
				if (!parseCiriFile(file, circRnas, sampleName, toolName, fileName)) {
					throw new FileReadException("Can not open [" + file.getName() + "] or file FORMAT ERROR!");
				}
			} else if (toolName.equalsIgnoreCase(Constant.TOOL_CIRCEXPLORER)) {
				if (!parseCircExplorerFile(file, circRnas, sampleName, toolName, fileName)) {
					throw new FileReadException("Can not open [" + file.getName() + "] or file FORMAT ERROR!");
				}
			} else if (toolName.equalsIgnoreCase(Constant.TOOL_MAPSPLICE)) {
				if (!parseMapspliceFile(file, circRnas, sampleName, toolName, fileName)) {
					throw new FileReadException("Can not open [" + file.getName() + "] or file FORMAT ERROR!");
				}
			} else if (toolName.equalsIgnoreCase(Constant.TOOL_UROBORUS)) {
				if (!parseUroborusFile(file, circRnas, sampleName, toolName, fileName)) {
					throw new FileReadException("Can not open [" + file.getName() + "] or file FORMAT ERROR!");
				}
			} else {
				if (!parseCircRnaFinderFile(file, circRnas, sampleName, toolName, fileName)) {
					throw new FileReadException("Can not open [" + file.getName() + "] or file FORMAT ERROR!");
				}

			}
		}
		CircView.log.info("Genes number: " + genes.size());
		CircView.log.info("CircRNAs number: " + circRnas.size());
		assignAll(genes, circRnas);
		return true;
	}

	private static boolean parseGeneTranscriptFile(File file, TreeMap<String, Gene> genes) {
		boolean ret = true;
		CircView.log.info("Parsing " + file.getName());
		BufferedReader reader = null;
		try {
			if (file.isFile() && file.exists()) {
				reader = new BufferedReader(new FileReader(file));
				String lineTxt = null;
				int lineNum = 0;
				while ((lineTxt = reader.readLine()) != null) {
					lineNum++;
					if (lineTxt.toLowerCase().contains("gene")) {
						continue;
					}
					String[] parts = lineTxt.split("\t");
					// parts[0] Gene Name
					// parts[1] Gene Transcript Name
					// parts[2] Chromosome name
					// parts[3] + or - for strand
					// parts[4] Transcription start position
					// parts[5] Transcription end position
					// parts[6] Coding region start
					// parts[7] Coding region end
					// parts[8] Number of exons
					// parts[9] Exon start postions
					// parts[10] Exon end postions
					if (parts[3].contains("+") || parts[3].contains("-")) {
					} else if (1 == lineNum) {
						continue;
					} else {
						reader.close();
						return false;
					}
					String geneName = parts[0];
					GeneTranscript geneTranscript = new GeneTranscript(geneName);
					geneTranscript.setTranscriptName(parts[1]);
					geneTranscript.setChrom(parts[2]);
					geneTranscript.setStrand(parts[3]);
					geneTranscript.setTxStart(Long.parseLong(parts[4]));
					geneTranscript.setTxEnd(Long.parseLong(parts[5]));
					geneTranscript.setCdsStart(Long.parseLong(parts[6]));
					geneTranscript.setCdsEnd(Long.parseLong(parts[7]));
					geneTranscript.setExonCount(Integer.parseInt(parts[8]));
					String[] exonStarts = parts[9].split(",");
					for (int i = 0; i < exonStarts.length; i++) {
						geneTranscript.getExonStarts().addElement(Long.parseLong(exonStarts[i]));
					}
					String[] exonEnds = parts[10].split(",");
					for (int i = 0; i < exonEnds.length; i++) {
						geneTranscript.getExonEnds().addElement(Long.parseLong(exonEnds[i]));
					}
					if (genes.containsKey(geneName.toUpperCase())) {
						genes.get(geneName.toUpperCase()).getGeneTranscripts().put(geneTranscript.getTranscriptName(),
								geneTranscript);
					} else {
						Gene gene = new Gene(geneName);
						gene.getGeneTranscripts().put(geneTranscript.getTranscriptName(), geneTranscript);
						genes.put(geneName.toUpperCase(), gene);
					}
				}
				reader.close();
			} else {
				ret = false;
				CircView.log.info("Can't open the file: " + file.getName());
			}
		} catch (Exception e) {
			ret = false;
			CircView.log.warn(e.getMessage());
		}
		return ret;
	}

	private static boolean parseCircRnaFinderFile(File file, TreeMap<String, CircRna> circRnas, String sampleName,
			String circTool, String fileName) {
		boolean ret = true;
		CircView.log.info("Parsing " + file.getName());
		BufferedReader reader = null;
		try {
			if (file.isFile() && file.exists()) {
				reader = new BufferedReader(new FileReader(file));
				String lineTxt = null;
				int lineNum = 0;
				while ((lineTxt = reader.readLine()) != null) {
					lineNum++;
					if (lineTxt.toLowerCase().contains("strand")) {
						continue;
					}
					String[] parts = lineTxt.split("\t");
					// parts[0] Chromosome name
					// parts[1] start position
					// parts[2] end position
					// parts[3]
					// parts[4] junction reads
					// parts[5] + or - for strand
					if (parts[5].contains("+") || parts[5].contains("-")) {
					} else if (1 == lineNum) {
						continue;
					} else {
						reader.close();
						return false;
					}
					String circRnaId = parts[0] + ":" + parts[1] + "|" + parts[2];
					CircRna circRna = new CircRna(circRnaId);
					circRna.setChrom(parts[0]);
					circRna.setStartPoint(Long.parseLong(parts[1]));
					circRna.setEndPoint(Long.parseLong(parts[2]));
					circRna.setJunctionReads((int) Long.parseLong(parts[4]));
					circRna.setStrand(parts[5]);
					if (circRnas.containsKey(circRnaId.toUpperCase())) {
						if (null != circRnas.get(circRnaId.toUpperCase()).getSamples().get(sampleName)) {
							circRnas.get(circRnaId.toUpperCase()).getSamples().put(sampleName,
									circRnas.get(circRnaId.toUpperCase()).getSamples().get(sampleName) + 1);
						} else {
							circRnas.get(circRnaId.toUpperCase()).getSamples().put(sampleName, 1);
						}
						if (null != circRnas.get(circRnaId.toUpperCase()).getCircTools().get(circTool)) {
							circRnas.get(circRnaId.toUpperCase()).getCircTools().put(circTool,
									circRnas.get(circRnaId.toUpperCase()).getCircTools().get(circTool) + 1);
						} else {
							circRnas.get(circRnaId.toUpperCase()).getCircTools().put(circTool, 1);
						}
						if (null != circRnas.get(circRnaId.toUpperCase()).getFiles().get(fileName)) {
							circRnas.get(circRnaId.toUpperCase()).getFiles().put(fileName,
									circRnas.get(circRnaId.toUpperCase()).getFiles().get(fileName) + 1);
						} else {
							circRnas.get(circRnaId.toUpperCase()).getFiles().put(fileName, 1);
						}
						// Save the Max Junction reads
						if (circRna.getJunctionReads() > circRnas.get(circRnaId.toUpperCase()).getJunctionReads()) {
							circRnas.get(circRnaId.toUpperCase()).setJunctionReads(circRna.getJunctionReads());
						}
					} else {
						circRna.getSamples().put(sampleName, 1);
						circRna.getCircTools().put(circTool, 1);
						circRna.getFiles().put(fileName, 1);
						circRnas.put(circRnaId.toUpperCase(), circRna);
					}
				}
				reader.close();
			} else {
				CircView.log.warn("Can't find the file: " + file.getName());
			}
		} catch (Exception e) {
			CircView.log.warn(e.getMessage());
			ret = false;
		}
		return ret;
	}

	private static boolean parseCircExplorerFile(File file, TreeMap<String, CircRna> circRnas, String sampleName,
			String circTool, String fileName) {
		boolean ret = true;
		CircView.log.info("Parsing " + file.getName());
		BufferedReader reader = null;
		try {
			if (file.isFile() && file.exists()) {
				reader = new BufferedReader(new FileReader(file));
				String lineTxt = null;
				int lineNum = 0;
				while ((lineTxt = reader.readLine()) != null) {
					lineNum++;
					String[] parts = lineTxt.split("\t");
					// parts[0] Chromosome name
					// parts[1] start position
					// parts[2] end position
					// parts[3] CircRNA/junction reads
					// parts[4]
					// parts[5] + or - for strand
					if (parts[5].contains("+") || parts[5].contains("-")) {
					} else if (1 == lineNum) {
						continue;
					} else {
						reader.close();
						return false;
					}
					String circRnaId = parts[0] + ":" + parts[1] + "|" + parts[2];
					CircRna circRna = new CircRna(circRnaId);
					circRna.setChrom(parts[0]);
					circRna.setStartPoint(Long.parseLong(parts[1]));
					circRna.setEndPoint(Long.parseLong(parts[2]));
					String tmp = parts[3];
					String[] junc = tmp.split("/");
					circRna.setJunctionReads((int) Long.parseLong(junc[1]));
					circRna.setStrand(parts[5]);
					if (circRnas.containsKey(circRnaId.toUpperCase())) {
						if (null != circRnas.get(circRnaId.toUpperCase()).getSamples().get(sampleName)) {
							circRnas.get(circRnaId.toUpperCase()).getSamples().put(sampleName,
									circRnas.get(circRnaId.toUpperCase()).getSamples().get(sampleName) + 1);
						} else {
							circRnas.get(circRnaId.toUpperCase()).getSamples().put(sampleName, 1);
						}
						if (null != circRnas.get(circRnaId.toUpperCase()).getCircTools().get(circTool)) {
							circRnas.get(circRnaId.toUpperCase()).getCircTools().put(circTool,
									circRnas.get(circRnaId.toUpperCase()).getCircTools().get(circTool) + 1);
						} else {
							circRnas.get(circRnaId.toUpperCase()).getCircTools().put(circTool, 1);
						}
						if (null != circRnas.get(circRnaId.toUpperCase()).getFiles().get(fileName)) {
							circRnas.get(circRnaId.toUpperCase()).getFiles().put(fileName,
									circRnas.get(circRnaId.toUpperCase()).getFiles().get(fileName) + 1);
						} else {
							circRnas.get(circRnaId.toUpperCase()).getFiles().put(fileName, 1);
						}
						// Save the Max Junction reads
						if (circRna.getJunctionReads() > circRnas.get(circRnaId.toUpperCase()).getJunctionReads()) {
							circRnas.get(circRnaId.toUpperCase()).setJunctionReads(circRna.getJunctionReads());
						}
					} else {
						circRna.getSamples().put(sampleName, 1);
						circRna.getCircTools().put(circTool, 1);
						circRna.getFiles().put(fileName, 1);
						circRnas.put(circRnaId.toUpperCase(), circRna);
					}
				}
				reader.close();
			} else {
				CircView.log.warn("Can't find the file: " + file.getName());
			}
		} catch (Exception e) {
			CircView.log.warn(e.getMessage());
			ret = false;
		}
		return ret;
	}

	private static boolean parseCiriFile(File file, TreeMap<String, CircRna> circRnas, String sampleName,
			String circTool, String fileName) {
		boolean ret = true;
		CircView.log.info("Parsing " + file.getName());
		BufferedReader reader = null;
		try {
			if (file.isFile() && file.exists()) {
				reader = new BufferedReader(new FileReader(file));
				String lineTxt = null;
				int lineNum = 0;
				while ((lineTxt = reader.readLine()) != null) {
					lineNum++;
					String[] parts = lineTxt.split("\t");
					// parts[0] CircRNA ID
					// parts[1] Chromosome name
					// parts[2] start position
					// parts[3] end position
					// parts[4] junction reads
					// parts[10] + or - for strand
					if (parts[10].contains("+") || parts[10].contains("-")) {
					} else if (1 == lineNum) {
						continue;
					} else {
						reader.close();
						return false;
					}
					String circRnaId = parts[0];
					CircRna circRna = new CircRna(circRnaId);
					circRna.setChrom(parts[1]);
					circRna.setStartPoint(Long.parseLong(parts[2]));
					circRna.setEndPoint(Long.parseLong(parts[3]));
					circRna.setJunctionReads((int) Long.parseLong(parts[4]));
					circRna.setStrand(parts[10]);
					if (circRnas.containsKey(circRnaId.toUpperCase())) {
						if (null != circRnas.get(circRnaId.toUpperCase()).getSamples().get(sampleName)) {
							circRnas.get(circRnaId.toUpperCase()).getSamples().put(sampleName,
									circRnas.get(circRnaId.toUpperCase()).getSamples().get(sampleName) + 1);
						} else {
							circRnas.get(circRnaId.toUpperCase()).getSamples().put(sampleName, 1);
						}
						if (null != circRnas.get(circRnaId.toUpperCase()).getCircTools().get(circTool)) {
							circRnas.get(circRnaId.toUpperCase()).getCircTools().put(circTool,
									circRnas.get(circRnaId.toUpperCase()).getCircTools().get(circTool) + 1);
						} else {
							circRnas.get(circRnaId.toUpperCase()).getCircTools().put(circTool, 1);
						}
						if (null != circRnas.get(circRnaId.toUpperCase()).getFiles().get(fileName)) {
							circRnas.get(circRnaId.toUpperCase()).getFiles().put(fileName,
									circRnas.get(circRnaId.toUpperCase()).getFiles().get(fileName) + 1);
						} else {
							circRnas.get(circRnaId.toUpperCase()).getFiles().put(fileName, 1);
						}
						// Save the Max Junction reads
						if (circRna.getJunctionReads() > circRnas.get(circRnaId.toUpperCase()).getJunctionReads()) {
							circRnas.get(circRnaId.toUpperCase()).setJunctionReads(circRna.getJunctionReads());
						}
					} else {
						circRna.getSamples().put(sampleName, 1);
						circRna.getCircTools().put(circTool, 1);
						circRna.getFiles().put(fileName, 1);
						circRnas.put(circRnaId.toUpperCase(), circRna);
					}
				}
				reader.close();
			} else {
				CircView.log.warn("Can't find the file: " + file.getName());
				ret = false;
			}
		} catch (Exception e) {
			CircView.log.warn(e.getMessage());
			ret = false;
		}
		return ret;
	}

	private static boolean parseMapspliceFile(File file, TreeMap<String, CircRna> circRnas, String sampleName,
			String circTool, String fileName) {
		boolean ret = true;
		CircView.log.info("Parsing " + file.getName());
		BufferedReader reader = null;
		try {
			if (file.isFile() && file.exists()) {
				reader = new BufferedReader(new FileReader(file));
				String lineTxt = null;
				int lineNum = 0;
				while ((lineTxt = reader.readLine()) != null) {
					lineNum++;
					if (lineTxt.toLowerCase().contains("circ")) {
						continue;
					}
					String[] parts = lineTxt.split("\t");
					// parts[0] Chromosome name
					// parts[1] start position
					// parts[2] end position
					// parts[3]
					// parts[4] junction reads
					// parts[5] + or - for strand
					// parts[6]
					if (parts[5].contains("+") || parts[5].contains("-")) {
					} else if (1 == lineNum) {
						continue;
					} else {
						reader.close();
						return false;
					}
					String[] tmp = parts[0].split("~");
					String chr = tmp[0];
					String circRnaId = chr + ":" + parts[1] + "|" + parts[2];
					CircRna circRna = new CircRna(circRnaId);
					circRna.setChrom(chr);
					circRna.setStartPoint(Long.parseLong(parts[1]));
					circRna.setEndPoint(Long.parseLong(parts[2]));
					circRna.setStrand(parts[5].substring(0, 1));
					circRna.setJunctionReads((int) Long.parseLong(parts[4]));
					if (circRnas.containsKey(circRnaId.toUpperCase())) {
						if (null != circRnas.get(circRnaId.toUpperCase()).getSamples().get(sampleName)) {
							circRnas.get(circRnaId.toUpperCase()).getSamples().put(sampleName,
									circRnas.get(circRnaId.toUpperCase()).getSamples().get(sampleName) + 1);
						} else {
							circRnas.get(circRnaId.toUpperCase()).getSamples().put(sampleName, 1);
						}
						if (null != circRnas.get(circRnaId.toUpperCase()).getCircTools().get(circTool)) {
							circRnas.get(circRnaId.toUpperCase()).getCircTools().put(circTool,
									circRnas.get(circRnaId.toUpperCase()).getCircTools().get(circTool) + 1);
						} else {
							circRnas.get(circRnaId.toUpperCase()).getCircTools().put(circTool, 1);
						}
						if (null != circRnas.get(circRnaId.toUpperCase()).getFiles().get(fileName)) {
							circRnas.get(circRnaId.toUpperCase()).getFiles().put(fileName,
									circRnas.get(circRnaId.toUpperCase()).getFiles().get(fileName) + 1);
						} else {
							circRnas.get(circRnaId.toUpperCase()).getFiles().put(fileName, 1);
						}
						// Save the Max Junction reads
						if (circRna.getJunctionReads() > circRnas.get(circRnaId.toUpperCase()).getJunctionReads()) {
							circRnas.get(circRnaId.toUpperCase()).setJunctionReads(circRna.getJunctionReads());
						}
					} else {
						circRna.getSamples().put(sampleName, 1);
						circRna.getCircTools().put(circTool, 1);
						circRna.getFiles().put(fileName, 1);
						circRnas.put(circRnaId.toUpperCase(), circRna);
					}
				}
				reader.close();
			} else {
				CircView.log.warn("Can't find the file: " + file.getName());
			}
		} catch (Exception e) {
			CircView.log.warn(e.getMessage());
			ret = false;
		}
		return ret;
	}

	private static boolean parseUroborusFile(File file, TreeMap<String, CircRna> circRnas, String sampleName,
			String circTool, String fileName) {
		boolean ret = true;
		CircView.log.info("Parsing " + file.getName());
		BufferedReader reader = null;
		try {
			if (file.isFile() && file.exists()) {
				reader = new BufferedReader(new FileReader(file));
				String lineTxt = null;
				int lineNum = 0;
				while ((lineTxt = reader.readLine()) != null) {
					lineNum++;
					if (lineTxt.toLowerCase().contains("circ")) {
						continue;
					}
					String[] parts = lineTxt.split("\t");
					// parts[0] Chromosome name
					// parts[1] start position
					// parts[2] end position
					// parts[3] + or - for strand
					// parts[4]
					// parts[5]
					// parts[6] junction reads
					if (parts[3].contains("+") || parts[3].contains("-")) {
					} else if (1 == lineNum) {
						continue;
					} else {
						reader.close();
						return false;
					}
					String circRnaId = parts[0] + ":" + parts[1] + "|" + parts[2];
					CircRna circRna = new CircRna(circRnaId);
					circRna.setChrom(parts[0]);
					circRna.setStartPoint(Long.parseLong(parts[1]));
					circRna.setEndPoint(Long.parseLong(parts[2]));
					circRna.setStrand(parts[3]);
					circRna.setJunctionReads((int) Long.parseLong(parts[6]));
					if (circRnas.containsKey(circRnaId.toUpperCase())) {
						if (null != circRnas.get(circRnaId.toUpperCase()).getSamples().get(sampleName)) {
							circRnas.get(circRnaId.toUpperCase()).getSamples().put(sampleName,
									circRnas.get(circRnaId.toUpperCase()).getSamples().get(sampleName) + 1);
						} else {
							circRnas.get(circRnaId.toUpperCase()).getSamples().put(sampleName, 1);
						}
						if (null != circRnas.get(circRnaId.toUpperCase()).getCircTools().get(circTool)) {
							circRnas.get(circRnaId.toUpperCase()).getCircTools().put(circTool,
									circRnas.get(circRnaId.toUpperCase()).getCircTools().get(circTool) + 1);
						} else {
							circRnas.get(circRnaId.toUpperCase()).getCircTools().put(circTool, 1);
						}
						if (null != circRnas.get(circRnaId.toUpperCase()).getFiles().get(fileName)) {
							circRnas.get(circRnaId.toUpperCase()).getFiles().put(fileName,
									circRnas.get(circRnaId.toUpperCase()).getFiles().get(fileName) + 1);
						} else {
							circRnas.get(circRnaId.toUpperCase()).getFiles().put(fileName, 1);
						}
						// Save the Max Junction reads
						if (circRna.getJunctionReads() > circRnas.get(circRnaId.toUpperCase()).getJunctionReads()) {
							circRnas.get(circRnaId.toUpperCase()).setJunctionReads(circRna.getJunctionReads());
						}
					} else {
						circRna.getSamples().put(sampleName, 1);
						circRna.getCircTools().put(circTool, 1);
						circRna.getFiles().put(fileName, 1);
						circRnas.put(circRnaId.toUpperCase(), circRna);
					}
				}
				reader.close();
			} else {
				CircView.log.warn("Can't find the file: " + file.getName());
			}
		} catch (Exception e) {
			CircView.log.warn(e.getMessage());
			ret = false;
		}
		return ret;
	}

	private static void preAssign(TreeMap<String, Gene> allGene, TreeMap<String, ArrayList<Long>> txStartMap,
			TreeMap<String, ArrayList<Long>> txEndMap, TreeMap<Long, TreeMap<String, String>> txStart2Name,
			TreeMap<Long, TreeMap<String, String>> txEnd2Name) {

		// Init Maps
		txStartMap.clear();
		txEndMap.clear();
		txStart2Name.clear();
		txEnd2Name.clear();

		// Create Chrom -- > txStart-geneNames pair
		for (String geneName : allGene.keySet()) {
			String chrom = "";
			TreeMap<Long, Integer> tmpUniqueStart = new TreeMap<Long, Integer>();
			TreeMap<Long, Integer> tmpUniqueEnd = new TreeMap<Long, Integer>();
			TreeMap<String, GeneTranscript> transcripts = allGene.get(geneName.toUpperCase()).getGeneTranscripts();
			for (String transcriptName : transcripts.keySet()) {
				GeneTranscript transcript = transcripts.get(transcriptName);
				chrom = transcript.getChrom();
				Long txStart = transcript.getTxStart();
				Long txEnd = transcript.getTxEnd();
				if (txStart2Name.get(txStart) != null) {
					TreeMap<String, String> genesTmp = txStart2Name.get(txStart);
					genesTmp.put(geneName, geneName);
					txStart2Name.put(txStart, genesTmp);
				} else {
					TreeMap<String, String> genesTmp = new TreeMap<String, String>();
					genesTmp.put(geneName, geneName);
					txStart2Name.put(txStart, genesTmp);
				}
				if (txEnd2Name.get(txEnd) != null) {
					TreeMap<String, String> genesTmp = txEnd2Name.get(txEnd);
					genesTmp.put(geneName, geneName);
					txEnd2Name.put(txEnd, genesTmp);
				} else {
					TreeMap<String, String> genesTmp = new TreeMap<String, String>();
					genesTmp.put(geneName, geneName);
					txEnd2Name.put(txEnd, genesTmp);
				}
				tmpUniqueStart.put(txStart, 1);
				tmpUniqueEnd.put(txEnd, 1);
			}
			// Create Unique txStart and txEnd list
			ArrayList<Long> uniqueStartList = new ArrayList<Long>();
			ArrayList<Long> uniqueEndList = new ArrayList<Long>();
			for (Long txStart : tmpUniqueStart.keySet()) {
				uniqueStartList.add(txStart);
			}
			for (Long txEnd : tmpUniqueEnd.keySet()) {
				uniqueEndList.add(txEnd);
			}
			// Create Chrom --> Unique List
			if (txStartMap.get(chrom) != null) {
				txStartMap.get(chrom).addAll(uniqueStartList);
			} else {
				txStartMap.put(chrom, uniqueStartList);
			}
			if (txEndMap.get(chrom) != null) {
				txEndMap.get(chrom).addAll(uniqueEndList);
			} else {
				txEndMap.put(chrom, uniqueEndList);
			}
		}

		// Sort Unique List
		for (String chr : txStartMap.keySet()) {
			Collections.sort(txStartMap.get(chr));
		}
		for (String chr : txEndMap.keySet()) {
			Collections.sort(txEndMap.get(chr));
		}
	}

	private static void assign(CircRna circRna, TreeMap<String, Gene> allGene,
			TreeMap<String, ArrayList<Long>> chr2StartList, TreeMap<String, ArrayList<Long>> chr2EndList,
			TreeMap<Long, TreeMap<String, String>> txStart2Name, TreeMap<Long, TreeMap<String, String>> txEnd2Name) {
		String chrom = circRna.getChrom();
		Long startPoint = circRna.getStartPoint();
		Long endPoint = circRna.getEndPoint();
		ArrayList<Long> txStartList = chr2StartList.get(chrom);
		if (txStartList == null) {
			CircView.log.info("circRNA_ID [" + circRna.getCircRnaID() + "] can not find related chrom");
			return;
		}
		ArrayList<Long> txEndList = chr2EndList.get(chrom);
		if (txEndList == null) {
			CircView.log.info("circRNA_ID [" + circRna.getCircRnaID() + "] can not find related chrom");
			return;
		}
		// Binary search to find index of txStart
		int indexByTxStart = splitHalfByTxStart(txStartList, startPoint.longValue(), 0, txStartList.size() - 1);
		// index boundary control
		indexByTxStart = (indexByTxStart < 0) ? 0 : indexByTxStart;
		indexByTxStart = (indexByTxStart > txStartList.size() - 1) ? txStartList.size() - 1 : indexByTxStart;
		// Binary search to find index of txEnd
		int indexByTxEnd = splitHalfByTxEnd(txEndList, endPoint.longValue(), 0, txEndList.size() - 1);
		// index boundary control
		indexByTxEnd = (indexByTxEnd <= 1) ? 0 : indexByTxEnd;
		indexByTxEnd = (indexByTxEnd > txEndList.size() - 1) ? txEndList.size() - 1 : indexByTxEnd;

		TreeMap<String, Integer> geneNames = new TreeMap<String, Integer>();
		// Compare indexByTxStart and indexByTxEnd
		if ((indexByTxStart + 1) < (txEndList.size() - indexByTxEnd)) {
			// Select indexByTxStart
			for (int index = 0; index <= indexByTxStart; index++) {
				for (String name : txStart2Name.get(txStartList.get(index)).keySet()) {
					geneNames.put(name, 1);
				}
			}
		} else {
			// Select indexByTxEnd
			for (int index = indexByTxEnd; index < txEndList.size(); index++) {
				for (String name : txEnd2Name.get(txEndList.get(index)).keySet()) {
					geneNames.put(name, 1);
				}
			}
		}

		// gene location, circRNA type, region
		TreeMap<String, Integer> geneLocation = new TreeMap<String, Integer>();
		TreeMap<String, Integer> region = new TreeMap<String, Integer>();
		int exonSign = 0;
		for (String name : geneNames.keySet()) {
			int geneLocationSign = 0;
			int mRnaSign = 0;
			int lncRnaSign = 0;
			Gene gene = allGene.get(name.toUpperCase());
			TreeMap<String, GeneTranscript> transcripts;
			if (gene != null) {
				transcripts = gene.getGeneTranscripts();
			} else {
				return;
			}
			for (String transcriptName : transcripts.keySet()) {
				GeneTranscript trans = transcripts.get(transcriptName);
				if ((trans.getTxStart().longValue() <= startPoint.longValue())
						&& (endPoint.longValue() <= trans.getTxEnd().longValue())) {
					geneLocationSign++;
					geneLocation.put(name + " " + trans.getTranscriptName(), 1);

					if (trans.getCircRnas().get(circRna.getCircRnaID()) == null) {
						trans.getCircRnas().put(circRna.getCircRnaID(), circRna);
					}
					int repeat = 0;
					for (String fileName : circRna.getFiles().keySet()) {
						repeat += circRna.getFiles().get(fileName);
					}
					trans.getCircRnasNum().put(circRna.getCircRnaID(), repeat);
					trans.setTotalJunctionReads(trans.getTotalJunctionReads() + circRna.getJunctionReads());
				}
				if (trans.getCdsStart().longValue() < trans.getCdsEnd().longValue()) {
					mRnaSign++;
				} else {
					lncRnaSign++;
				}
				for (int i = 0; i < trans.getExonCount(); i++) {
					Long exonStart = trans.getExonStarts().get(i);
					Long exonEnd = trans.getExonEnds().get(i);
					if (((exonStart.longValue() - Constant.ASSIGN_TOLERATION <= startPoint.longValue())
							&& (startPoint.longValue() < exonEnd.longValue() + Constant.ASSIGN_TOLERATION))
							|| ((exonStart.longValue() - Constant.ASSIGN_TOLERATION < endPoint.longValue())
									&& (endPoint.longValue() <= exonEnd.longValue() + Constant.ASSIGN_TOLERATION))
							|| ((startPoint.longValue() < exonStart.longValue() - Constant.ASSIGN_TOLERATION)
									&& (exonEnd.longValue() < endPoint.longValue() + Constant.ASSIGN_TOLERATION))) {
						exonSign++;
					}
				}
			}
			if (geneLocationSign > 0) {
				if (mRnaSign > 0) {
					region.put("mRNA", 1);
				} else {
					region.put("lncRNA", 1);
				}
			}
		}

		if (geneLocation.size() > 0) {
			for (String n : geneLocation.keySet()) {
				circRna.getGeneTranscrpits().put(n, n);
			}
			if (exonSign > 0) {
				circRna.setCircRnaType("exon");
			} else {
				circRna.setCircRnaType("intron");
			}
			if ((region.get("mRNA") != null) && (region.get("lncRNA") == null)) {
				circRna.setRegion("mRNA");
			} else if ((region.get("mRNA") == null) && (region.get("lncRNA") != null)) {
				circRna.setRegion("lncRNA");
			}
			if ((region.get("mRNA") != null) && (region.get("lncRNA") != null)) {
				circRna.setRegion("mRNA,lncRNA");
			}
		} else {
			circRna.getGeneTranscrpits().put("n/a", "n/a");
			circRna.setCircRnaType("intergenic");
			circRna.setRegion("Unknown");
		}
	}

	private static void assignAll(TreeMap<String, Gene> allGene, TreeMap<String, CircRna> allCircRna) {
		TreeMap<String, ArrayList<Long>> chr2StartList = new TreeMap<String, ArrayList<Long>>();
		TreeMap<String, ArrayList<Long>> chr2EndList = new TreeMap<String, ArrayList<Long>>();
		TreeMap<Long, TreeMap<String, String>> txStart2Name = new TreeMap<Long, TreeMap<String, String>>();
		TreeMap<Long, TreeMap<String, String>> txEnd2Name = new TreeMap<Long, TreeMap<String, String>>();

		if (allGene != null && allCircRna != null) {
			preAssign(allGene, chr2StartList, chr2EndList, txStart2Name, txEnd2Name);
		}
		int num = 0;

		for (String circRnaId : allCircRna.keySet()) {
			if (0 == (++num % 1000)) {
				CircView.log.info("Mapping: [" + num + "|" + allCircRna.size() + "]");
			}
			CircRna circRna = allCircRna.get(circRnaId);
			assign(circRna, allGene, chr2StartList, chr2EndList, txStart2Name, txEnd2Name);
		}
	}

	private static int splitHalfByTxStart(ArrayList<Long> arrayData, Long txStart, int start, int end) {
		int index = (start + end) / 2;
		Long data = arrayData.get(index);
		if (start > end) {
			return end;
		}
		if (data.longValue() == txStart.longValue()) {
			return index;
		} else {
			if (data.longValue() < txStart.longValue()) {
				return splitHalfByTxStart(arrayData, txStart, index + 1, end);
			} else {
				return splitHalfByTxStart(arrayData, txStart, start, index - 1);
			}
		}
	}

	private static int splitHalfByTxEnd(ArrayList<Long> arrayData, Long txEnd, int start, int end) {
		int index = (start + end) / 2;
		Long data = arrayData.get(index);
		if (start > end) {
			return start;
		}
		if (data.longValue() == txEnd.longValue()) {
			return index;
		} else {
			if (data.longValue() < txEnd.longValue()) {
				return splitHalfByTxEnd(arrayData, txEnd, index + 1, end);
			} else {
				return splitHalfByTxEnd(arrayData, txEnd, start, index - 1);
			}
		}
	}
}
