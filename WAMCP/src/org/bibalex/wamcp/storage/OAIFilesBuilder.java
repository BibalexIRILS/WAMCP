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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.bibalex.gallery.exception.BAGException;
import org.bibalex.gallery.storage.BAGStorage;
import org.bibalex.jdom.XPathBuilder;
import org.bibalex.oai.DCTypes;
import org.bibalex.oai.oaidc.jaxb.ElementType;
import org.bibalex.oai.oaidc.jaxb.OaiDcType;
import org.bibalex.oai.oaidc.jaxb.ObjectFactory;
import org.bibalex.oai.util.BAOAIIDBuilder;
import org.bibalex.sdxe.controller.DomTreeController;
import org.bibalex.sdxe.xsa.exception.XSAException;
import org.bibalex.sdxe.xsa.model.XSADocument;
import org.bibalex.sdxe.xsa.model.XSAInstance;
import org.bibalex.util.JDOMUtils;
import org.bibalex.util.URLPathStrUtils;
import org.bibalex.wamcp.exception.WAMCPException;
import org.bibalex.wamcp.exception.WAMCPGeneralCorrectableException;
import org.dspace.foresite.Agent;
import org.dspace.foresite.AggregatedResource;
import org.dspace.foresite.Aggregation;
import org.dspace.foresite.OREException;
import org.dspace.foresite.OREFactory;
import org.dspace.foresite.ORESerialiser;
import org.dspace.foresite.ORESerialiserException;
import org.dspace.foresite.ORESerialiserFactory;
import org.dspace.foresite.Predicate;
import org.dspace.foresite.ResourceMap;
import org.dspace.foresite.ResourceMapDocument;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import com.sun.xml.internal.bind.marshaller.NamespacePrefixMapper;

public class OAIFilesBuilder {
	private static final String CONST_PFX_DCTERMS = "dcterms";
	private static final String CONST_NSURI_DCTERMS = "http://purl.org/dc/terms";
	
	protected static void releaseOAIFiles(Document metadataXml, URI bagApiUri, String fileName,
			XSADocument xsaDoc, String galleryRootUrlStr) throws WAMCPException, BAGException {
		String oaimetUrlstr = URLPathStrUtils.appendParts(
				galleryRootUrlStr, "OAIMeta");
		
		URI aggrUri = bagApiUri.resolve("item/");
		URI descUri = bagApiUri.resolve("desc/");
		URI facsUri = bagApiUri.resolve("image/");
		
		Namespace nsXP = DomTreeController.acquireNamespace(metadataXml.getRootElement()
				.getNamespace().getURI(), metadataXml.getRootElement(), true);
		
		BAOAIIDBuilder idBuilder = new BAOAIIDBuilder("wamcp.bibalex.org");
		String oaiIdMSS = idBuilder.buildId(fileName);
		
		// Write an ORE file
		
		Aggregation aggManuscript;
		ResourceMap remManuscript;
		
		try {
			
			Predicate predDesc = new Predicate();
			predDesc.setName("description");
			predDesc.setURI(new URI(URLPathStrUtils.appendParts(CONST_NSURI_DCTERMS, "description")));
			predDesc.setNamespace(CONST_NSURI_DCTERMS);
			predDesc.setPrefix(CONST_PFX_DCTERMS);
			
			Predicate predSource = new Predicate();
			predSource.setName("source");
			predSource.setURI(new URI(URLPathStrUtils.appendParts(CONST_NSURI_DCTERMS, "source")));
			predSource.setNamespace(CONST_NSURI_DCTERMS);
			predSource.setPrefix(CONST_PFX_DCTERMS);
			
			Predicate predTitle = new Predicate();
			predTitle.setName("title");
			predTitle.setURI(new URI(URLPathStrUtils.appendParts(CONST_NSURI_DCTERMS, "title")));
			predTitle.setNamespace(CONST_NSURI_DCTERMS);
			predTitle.setPrefix(CONST_PFX_DCTERMS);
			
			Predicate predHasFormat = new Predicate();
			predHasFormat.setName("hasFormat");
			predHasFormat.setURI(new URI(URLPathStrUtils.appendParts(CONST_NSURI_DCTERMS,
					"hasFormat")));
			predHasFormat.setNamespace(CONST_NSURI_DCTERMS);
			predHasFormat.setPrefix(CONST_PFX_DCTERMS);
			
			Predicate predIsFormatOf = new Predicate();
			predIsFormatOf.setName("isFormatOf");
			predIsFormatOf.setURI(new URI(URLPathStrUtils.appendParts(CONST_NSURI_DCTERMS,
					"isFormatOf")));
			predIsFormatOf.setNamespace(CONST_NSURI_DCTERMS);
			predIsFormatOf.setPrefix(CONST_PFX_DCTERMS);
			
			Predicate predHasPart = new Predicate();
			predHasPart.setName("hasPart");
			predHasPart
					.setURI(new URI(URLPathStrUtils.appendParts(CONST_NSURI_DCTERMS, "hasPart")));
			predHasPart.setNamespace(CONST_NSURI_DCTERMS);
			predHasPart.setPrefix(CONST_PFX_DCTERMS);
			
			Predicate predIsPartOf = new Predicate();
			predIsPartOf.setName("isPartOf");
			predIsPartOf.setURI(new URI(URLPathStrUtils
					.appendParts(CONST_NSURI_DCTERMS, "isPartOf")));
			predIsPartOf.setNamespace(CONST_NSURI_DCTERMS);
			predIsPartOf.setPrefix(CONST_PFX_DCTERMS);
			
			URI mssUri = new URI(URLPathStrUtils.appendParts(aggrUri.toString(), oaiIdMSS));
			
			aggManuscript = OREFactory.createAggregation(mssUri);
			remManuscript = aggManuscript.createResourceMap(URI.create(URLPathStrUtils.appendParts(
					mssUri.toString(), "rdf")));
			
			Agent creatorWAMCP = OREFactory.createAgent();
			creatorWAMCP.addName(org.bibalex.Messages
					.getString("en", "WAMCPIndexedStorage.Creator"));
			
			remManuscript.addCreator(creatorWAMCP);
			
			XSAInstance keyInst = xsaDoc.getIndexedKey();
			
// String keyXpStr = keyInst.getXPathToAllOccsInAllConts();
			String keyXpStr = keyInst.getXPathToAllOccsInAllBaseShifts();
			keyXpStr = XPathBuilder.addNamespacePrefix(keyXpStr, nsXP);
			
			XPath keyXP = XPath.newInstance(keyXpStr);
			keyXP.addNamespace(nsXP);
			
			Object obj = keyXP.selectSingleNode(metadataXml.getRootElement());
			
			if (obj == null) {
				throw new WAMCPException("A record without Shelfmark!!");
			}
			
			String shelfmark = ((Element) obj).getTextNormalize();
			
			aggManuscript.addCreator(creatorWAMCP);
			aggManuscript
					.addTitle(org.bibalex.Messages.getString("en", "WAMCPIndexedStorage.DescTitle")
							+ shelfmark);
			
			aggManuscript.addRights(org.bibalex.Messages.getString("en",
					"WAMCPIndexedStorage.LicenseUri"));
			aggManuscript.addRights(bagApiUri.resolve("terms").toString());
			
			AggregatedResource arMetadata = aggManuscript
					.createAggregatedResource(URI.create(URLPathStrUtils.appendParts(
							descUri.toString(),
							oaiIdMSS)));
			arMetadata.addType(DCTypes.COLLECTION);
			
			AggregatedResource arMetadataTei = aggManuscript
					.createAggregatedResource(URI.create(URLPathStrUtils.appendParts(
							descUri.toString(),
							oaiIdMSS, "tei")));
			arMetadataTei.addType(DCTypes.TEXT);
			
			arMetadataTei.createTriple(predIsFormatOf, arMetadata);
			arMetadata.createTriple(predHasFormat, arMetadataTei);
			
			String facsIdsXpStr = "//facsimile/surface/@xml:id";
			facsIdsXpStr = XPathBuilder.addNamespacePrefix(facsIdsXpStr, nsXP);
			
			XPath facsIdsXp = XPath.newInstance(facsIdsXpStr);
			facsIdsXp.addNamespace(nsXP);
			
			for (Object facsIdObj : facsIdsXp.selectNodes(metadataXml)) {
				
				Attribute facsIdAttr = (Attribute) facsIdObj;
				String facsId = facsIdAttr.getValue();
				// Avoid making unnecessary changes to the way images are identified
// String facsOaiId = oaiIdMSS + "-" + facsId;
				String facsOaiId = oaiIdMSS + "_" + facsId.substring(1);
				
				AggregatedResource arFacsAggr = aggManuscript // aggFacs
						.createAggregatedResource(URI.create(URLPathStrUtils.appendParts(
								facsUri.toString(), facsOaiId)));
				
				arFacsAggr.addType(DCTypes.COLLECTION);
				
				AggregatedResource arFacsThumb = aggManuscript // aggFacs
						.createAggregatedResource(URI.create(URLPathStrUtils.appendParts(
								facsUri.toString(), facsOaiId, "thumb")));
				
				arFacsThumb.addType(DCTypes.TEXT);
				arFacsThumb.addType(DCTypes.IMAGE);
				
// Only one way to get the image is enough. This way crawlers can find the image, but the API contract
// is not repeated here
// AggregatedResource arFacsThumb = aggManuscript // aggFacs
// .createAggregatedResource(URI.create(URLPathStrUtils.appendParts(facsUri.toString(),facsOaiId, "thumb")));
// arFacsThumb.addType(DCTypes.TEXT);
// arFacsThumb.addType(DCTypes.IMAGE);
				
//
// AggregatedResource arFacsRegion = aggManuscript // aggFacs
// .createAggregatedResource(URI.create(URLPathStrUtils.appendParts(facsUri.toString(),facsOaiId, "region")));
// arFacsRegion.addType(DCTypes.TEXT);
// arFacsRegion.addType(DCTypes.SERVICE);
				
				AggregatedResource arFacsDesc = aggManuscript
						// aggFacs
						.createAggregatedResource(
						URI.create(URLPathStrUtils.appendParts(descUri.toString(), facsOaiId)));
				arFacsDesc.addType(DCTypes.COLLECTION);
				
				AggregatedResource arFacsDescTei = aggManuscript
						// aggFacs
						.createAggregatedResource(
						URI.create(URLPathStrUtils.appendParts(descUri.toString(), facsOaiId, "tei")));
				arFacsDescTei.addType(DCTypes.TEXT);
				
				arFacsDesc.createTriple(predHasFormat, arFacsDescTei);
				arFacsDescTei.createTriple(predIsFormatOf, arFacsDesc);
				
				// / Add dcterm relations to the aggregated resources
				
				// dcTerms for the Facs as a Collection of formats, ..etc
				arFacsAggr.createTriple(predTitle, "Facsimile " + facsId + " of manuscript "
						+ shelfmark);
				arFacsAggr.createTriple(predHasFormat, arFacsThumb);
				arFacsAggr.createTriple(predDesc, arFacsDesc);
				
				// dcterms for the format exposed to the crawlers
				arFacsThumb.createTriple(predIsFormatOf, arFacsAggr);
				
				// dcterms for the part/whole relation between tei record and its parts
				// dcterms for the part of the tei record that is related to the facs
				arFacsDesc.createTriple(predIsPartOf, arMetadata);
				arMetadata.createTriple(predHasPart, arFacsDesc);
				arFacsDesc.createTriple(predSource, arFacsAggr);
			}
			
			ORESerialiser serial = ORESerialiserFactory.getInstance("RDF/XML");
			ResourceMapDocument oreDoc;
			try {
				oreDoc = serial.serialise(remManuscript);
			} catch (ORESerialiserException e) {
				throw new WAMCPException(e);
			}
			String serialisation = oreDoc.toString();
			
			File oreTempFile = File.createTempFile("" + System.currentTimeMillis(), "oreReleased");
			FileOutputStream oreOut = new FileOutputStream(oreTempFile);
			
			oreOut.write(serialisation.getBytes());
			
			oreOut.flush();
			oreOut.close();
			
// String oreUrlStr = URLPathStrUtils.appendParts(
// oaimetUrlstr,
// fileName + ".ore.xml");
//
// if (!BAGStorage.putFile(oreUrlStr, oreTempFile, null)) {
// throw new WAMCPGeneralCorrectableException(
// "Could not upload file to release destination");
// }
			
			// Write a oaidc file
			ObjectFactory oaidcOF = new ObjectFactory();
			
			OaiDcType oaidcType = new OaiDcType();
			List<JAXBElement<ElementType>> aoidDcContent = oaidcType.getTitleOrCreatorOrSubject();
			
			JAXBElement<OaiDcType> oaiDcElement = oaidcOF.createDc(oaidcType);
			
			// //////////////////////////////////////////////
			ElementType idET = oaidcOF.createElementType();
			idET.setValue(mssUri.toString());
			aoidDcContent.add(oaidcOF.createIdentifier(idET));
			
			ElementType descET = oaidcOF.createElementType();
			StringBuilder mssDescBuilder = new StringBuilder();
			
			for (XSAInstance summaryInst : xsaDoc.getIndexedSummary()) {
				
				String summaryXpStr;
// summaryXpStr = summaryInst.getXPathToAllOccsInAllConts();
				summaryXpStr = summaryInst.getXPathToAllOccsInAllBaseShifts();
				
				summaryXpStr = XPathBuilder.addNamespacePrefix(summaryXpStr, nsXP);
				
				XPath summaryXP = XPath.newInstance(summaryXpStr);
				summaryXP.addNamespace(nsXP);
				
				List summaryNodes = summaryXP.selectNodes(metadataXml);
				
				if (summaryNodes.isEmpty()) {
					continue;
				}
				
				mssDescBuilder.append(summaryInst.getIxFieldName().replace('.', ' ') + ": ");
				
				for (Object summaryObj : summaryNodes) {
					
					mssDescBuilder.append(JDOMUtils.getNodeValue(summaryObj, true) + " | ");
					
				}
				
				mssDescBuilder.deleteCharAt(mssDescBuilder.length() - 1).append("|");
				
			}
			
			descET.setValue(mssDescBuilder.toString());
			aoidDcContent.add(oaidcOF.createDescription(descET));
			
			ElementType rightsET = oaidcOF.createElementType();
			rightsET.setValue(org.bibalex.Messages
					.getString("en", "WAMCPIndexedStorage.LicenseUri"));
			aoidDcContent.add(oaidcOF.createRights(rightsET));
			
			ElementType dctypeET = oaidcOF.createElementType();
			dctypeET.setValue(DCTypes.COLLECTION.toString());
			aoidDcContent.add(oaidcOF.createType(dctypeET));
			
			ElementType titleET = oaidcOF.createElementType();
			titleET.setValue(org.bibalex.Messages
					.getString("en", "WAMCPIndexedStorage.RecordTitle") + shelfmark);
			aoidDcContent.add(oaidcOF.createTitle(titleET));
			
			ElementType creatorET = oaidcOF.createElementType();
			creatorET.setValue(org.bibalex.Messages.getString("en", "WAMCPIndexedStorage.Creator"));
			aoidDcContent.add(oaidcOF.createCreator(creatorET));
			
			// ///////////////////////////////
			
			File oaidcTempFile = File.createTempFile(System.currentTimeMillis() + "",
					"oaidcReleased");
			FileOutputStream oaidcOut = new FileOutputStream(oaidcTempFile);
			
			JAXBContext context = JAXBContext.newInstance("org.bibalex.oai.oaidc.jaxb");
			
			XMLStreamWriter xmlStreamWriter = XMLOutputFactory.newInstance()
								.createXMLStreamWriter(oaidcOut);
			
// A work around for the lack of support of com.sun.xml.bind.namespacePrefixMapper, but it doesn't
// write the namespace declaration (logical, according to the javadoc)
// xmlStreamWriter.setPrefix("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/");
// xmlStreamWriter.setPrefix("dc", "http://purl.org/dc/elements/1.1/");
			
			Marshaller marshaller = context.createMarshaller();
			NamespacePrefixMapper namespacePrefixMapper = new NamespacePrefixMapper() {
				
				@Override
				public String getPreferredPrefix(String namespaceUri,
						String suggestion, boolean requirePrefix) {
					String result = null;
					if ("http://www.openarchives.org/OAI/2.0/oai_dc/"
							.equals(namespaceUri)) {
						result = "oai_dc";
					} else if ("http://purl.org/dc/elements/1.1/".equals(namespaceUri)) {
						result = "dc";
					}
					
					return result;
				}
			};
			
			try {
				marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper",
						namespacePrefixMapper);
			} catch (PropertyException e) {
				try {
					// The JAXB implementation shipped with JRE 6 has a wrong property name
					marshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper",
							namespacePrefixMapper);
				} catch (PropertyException e1) {
					// if the JAXB provider doesn't recognize the prefix mapper,
					// it will throw this exception. Since being unable to specify
					// a human friendly prefix is not really a fatal problem,
					// you can just continue marshalling without failing
					;
				}
			}
			
			// this is a JAXB standard property, so it should work with any JAXB impl.
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
			marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			
			marshaller.marshal(oaiDcElement, xmlStreamWriter);
			
			oaidcOut.flush();
			oaidcOut.close();
			
			// //////////////////////////////////
			
// String oaidcUrlStr = URLPathStrUtils.appendParts(
// oaimetUrlstr,
// fileName + ".oaidc.xml");
//
// if (!BAGStorage.putFile(oaidcUrlStr, oaidcTempFile, null)) {
// throw new WAMCPGeneralCorrectableException(
// "Could not upload file to release destination");
// }
			
			// ///////////////////////////////////////
			
			org.jdom.input.SAXBuilder jdomBuilder = new SAXBuilder();
			
			jdomBuilder.setFeature("http://xml.org/sax/features/validation", false);
			jdomBuilder.setFeature("http://xml.org/sax/features/namespaces", true);
			jdomBuilder.setFeature("http://xml.org/sax/features/namespace-prefixes",
					true);
			// Unsupported: jdomBuilder.setFeature("http://xml.org/sax/features/xmlns-uris", false);
			
			org.jdom.Document jdomORE = jdomBuilder.build(new FileInputStream(oreTempFile));
			org.jdom.Document jdomOAIDC = jdomBuilder.build(new FileInputStream(oaidcTempFile));
			
			org.jdom.Document jdomPMH = new Document();
			
			Namespace oaiNS = Namespace.getNamespace("", "http://www.openarchives.org/OAI/2.0/");
			
			Element recordElt = new Element("record", oaiNS);
			jdomPMH.setRootElement(recordElt);
			
			Element headerElt = new Element("header", oaiNS);
			recordElt.addContent(headerElt);
			
			Element idElt = new Element("identifier", oaiNS);
			headerElt.addContent(idElt);
			idElt.setText(oaiIdMSS);
			
			Element datestampElt = new Element("datestamp", oaiNS);
			headerElt.addContent(datestampElt);
			datestampElt.setText(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
			
			String setNameXPStr = "/TEI/text/body/div/msDesc/msIdentifier/collection/collection";
			setNameXPStr = XPathBuilder.addNamespacePrefix(setNameXPStr, nsXP);
			XPath setNameXP = XPath.newInstance(setNameXPStr);
			setNameXP.addNamespace(nsXP);
			Object setNameObj = setNameXP.selectSingleNode(metadataXml);
			if (setNameObj != null) {
				String setName = ((Element) setNameObj).getTextNormalize();
				
				Element setSpecElt = new Element("setSpec", oaiNS);
				headerElt.addContent(setSpecElt);
				setSpecElt.setText(setName);
			}
			
			Element metadataElt = new Element("metadata", oaiNS);
			recordElt.addContent(metadataElt);
			metadataElt.addContent(jdomOAIDC.getRootElement().detach());
			metadataElt.addContent(jdomORE.getRootElement().detach());
			
			File pmhTempFile = File.createTempFile(System.currentTimeMillis() + "", "pmhReleased");
			FileWriter pmhWriter = new FileWriter(pmhTempFile);
			XMLOutputter pmhOutputter = new XMLOutputter(Format.getCompactFormat().setEncoding(
					"UTF-8"));
			pmhOutputter.output(jdomPMH, pmhWriter);
			
			pmhWriter.flush();
			pmhWriter.close();
			
			String pmhUrlStr = URLPathStrUtils.appendParts(
					oaimetUrlstr,
					fileName + ".xml");
			
			if (!BAGStorage.putFile(pmhUrlStr, pmhTempFile, null)) {
				throw new WAMCPGeneralCorrectableException(
						"Could not upload file to release destination");
			}
			
		} catch (OREException e) {
			throw new WAMCPException(e);
		} catch (JDOMException e) {
			throw new WAMCPException(e);
		} catch (XSAException e) {
			throw new WAMCPException(e);
		} catch (IOException e) {
			throw new WAMCPException(e);
		} catch (URISyntaxException e) {
			throw new WAMCPException(e);
		} catch (JAXBException e) {
			throw new WAMCPException(e);
		} catch (XMLStreamException e) {
			throw new WAMCPException(e);
		} catch (FactoryConfigurationError e) {
			throw new WAMCPException(e);
		}
		
	}
}
