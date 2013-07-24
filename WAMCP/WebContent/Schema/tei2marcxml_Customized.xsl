<!--
   Copyright 2013 Bibliotheca Alexandrina, Wellcome Trust Library

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->

<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:marc="http://www.loc.gov/MARC21/slim" xmlns:tei="http://www.tei-c.org/ns/1.0">
    <xsl:output method="xml" encoding="UTF-8" indent="no"/>

    <!--these are global variables to be inserted later-->
    
    <!--classmarks and collections-->
    <xsl:variable name="main-collection" select="//tei:collection[@type='main']"/>
    <xsl:variable name="sub-collection" select="//tei:collection[@type='sub']"/>
    <xsl:variable name="classmark" select="//tei:idno"/>
    

    <!--handles header and footer-->
    <xsl:template match="/">

        <marc:collection xmlns:marc="http://www.loc.gov/MARC21/slim"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd">

            <xsl:apply-templates select="//tei:msPart"/>


        </marc:collection>

    </xsl:template>

    <!--all Wellcome Records have msPart(s)-->
    <xsl:template match="//tei:msPart">

        <xsl:apply-templates select="tei:msContents/tei:msItem[not(@n)]"/>

    </xsl:template>


    <!--pick up all non-table of contents msItems-->
    <xsl:template match="tei:msItem[not(@n)]">
        
        <!--one marc record for each-->
        <marc:record>

            <marc:leader>00000ntm a22000002a 4500</marc:leader>

                
            <!--deals with authors-->
            <xsl:apply-templates select="tei:author"/>
            
            <!--picks up 245-->            
            <xsl:apply-templates select="tei:title[@type='normalised']"/>
            
            <!--pick up english title-->
            <xsl:apply-templates select="tei:title[@type='other']"/>

            <!--pick up the incipit
            <xsl:apply-templates select="tei:incipit"/>-->
            <!--pick up the explicit
            <xsl:apply-templates select="tei:explicit"/>-->
            
            <!--picks up notes
            <xsl:apply-templates select="tei:note"/>
            -->
            
            <!--picks up arabic title-->
            <xsl:apply-templates select="tei:title[@type='original']"/>

            <!--picks up any physical description in the msPart-->
           <!-- <xsl:apply-templates select="../../tei:physDesc"/>-->
            
			<!-- and then looks for language of description in proper place
			 <xsl:apply-templates select="//tei:msDesc"/>-->
			
            <!-- and then looks for physical description in proper place-->
            <xsl:apply-templates select="//tei:msDesc/tei:physDesc"/>

            <!-- looks up history section-->
            <xsl:apply-templates select="//tei:msDesc/tei:history"/>
                
            <!-- classmark-->
            <xsl:apply-templates select="//tei:msDesc/tei:msIdentifier/tei:idno"/>

			 <!-- Michael: Language-->
            <xsl:apply-templates select="//tei:msDesc/tei:msPart/tei:msContents/tei:msItem/tei:textLang"/>

			
			 <!-- Michael: Language-->
          <!--
		  
		    <marc:datafield tag="856" ind1="" ind2=" ">
            <marc:subfield code="u">
               http://wamcp.bibalex.org
            </marc:subfield>
        </marc:datafield>
		  
		  -->
			
        </marc:record>

    </xsl:template>

    <!--process 100 and 700s-->
    
    <xsl:template match="tei:author">

        <xsl:apply-templates select="tei:persName"></xsl:apply-templates>
        
    </xsl:template>

    <xsl:template match="tei:persName">
        
        <!--what number author is this?-->
        <xsl:variable name="match-number"><xsl:number count="tei:author"/></xsl:variable>
        
        <xsl:choose>
            <!--is this the first match?-->
            <xsl:when test="$match-number=1">
                
                <xsl:choose>
                    <!--creates 100 field-->
                    <xsl:when test="@type='standard' and @xml:lang='eng-Latn'">
                        <marc:datafield tag="100" ind1="0" ind2=" ">                
                            <marc:subfield code="6">880-0<xsl:value-of select="$match-number"/></marc:subfield>
                            <marc:subfield code="a"><xsl:value-of select="normalize-space(.)"/></marc:subfield>
                        </marc:datafield>        
                    </xsl:when>
                    <!--and accompanying 880 field-->
                    <xsl:when test="@type='original'">
                        <marc:datafield tag="880" ind1="0" ind2=" ">                
                            <marc:subfield code="6">100-0<xsl:value-of select="$match-number"/></marc:subfield>
                            <marc:subfield code="a"><xsl:value-of select="normalize-space(.)"/></marc:subfield>
                        </marc:datafield>        
                    </xsl:when>
                </xsl:choose>
                
                
            </xsl:when>
            <xsl:otherwise>
                
                <xsl:choose>
                    <xsl:when test="@type='standard' and @xml:lang='eng-Latn'">
                        <marc:datafield tag="700" ind1="0" ind2=" ">
                            <marc:subfield code="6">880-0<xsl:value-of select="$match-number"/></marc:subfield>
                            <marc:subfield code="a"><xsl:value-of select="normalize-space(.)"/></marc:subfield>
                        </marc:datafield>        
                    </xsl:when>
                    <xsl:when test="@type='original'">
                        <marc:datafield tag="880" ind1="0" ind2=" ">
                            <marc:subfield code="6">700-0<xsl:value-of select="($match-number)-1"/></marc:subfield>
                            <marc:subfield code="a"><xsl:value-of select="normalize-space(.)"/></marc:subfield>
                        </marc:datafield>        
                    </xsl:when>
                </xsl:choose>
                
                
            </xsl:otherwise>
        </xsl:choose>
        
        
    </xsl:template>


    <!--processes title type=normalised-->
    <xsl:template match="tei:title[@type='normalised']">
        
        <xsl:variable select="count(../tei:author)+1" name="author-count"/>
        
        <xsl:choose>
            <xsl:when test="../tei:author">
        
                <marc:datafield tag="245" ind1="1" ind2="0">
                    <marc:subfield code="6">880-0<xsl:value-of select="$author-count"/></marc:subfield>
                    <marc:subfield code="a">
                        <xsl:value-of select="normalize-space(.)"
                        />
                    </marc:subfield>
                </marc:datafield>
                
            </xsl:when>
            <xsl:otherwise>
                
                <marc:datafield tag="245" ind1="0" ind2="0">
                    <marc:subfield code="6">880-01</marc:subfield>
                    <marc:subfield code="a">
                        <xsl:value-of select="normalize-space(.)"
                        />
                    </marc:subfield>
                </marc:datafield>
                
            </xsl:otherwise>
        </xsl:choose>
        
    </xsl:template>
    


    <!--processes title type=other-->
    <xsl:template match="tei:title[@type='other']">
        
        <marc:datafield tag="246" ind1="3" ind2=" ">
            <marc:subfield code="a">
                <xsl:value-of select="normalize-space(.)"/>
            </marc:subfield>
        </marc:datafield>
        
    </xsl:template>
    
    <!--processes title type=original-->
    <xsl:template match="tei:title[@type='original']">
        
        <xsl:choose>
            <xsl:when test="../tei:author">
        
                <marc:datafield tag="880" ind1="1" ind2="0">
                    <marc:subfield code="6">245-01</marc:subfield>
                    <marc:subfield code="a">
                        <xsl:value-of select="normalize-space(.)"/>
                    </marc:subfield>
                </marc:datafield>
        
            </xsl:when>
            <xsl:otherwise>
                <marc:datafield tag="880" ind1="0" ind2="0">
                    <marc:subfield code="6">245-01</marc:subfield>
                    <marc:subfield code="a">
                        <xsl:value-of select="normalize-space(.)"/>
                    </marc:subfield>
                </marc:datafield>
                
            </xsl:otherwise>
        </xsl:choose>
        
        
        
        
    </xsl:template>
    
    
    <!--processes incipit-->
    <xsl:template match="tei:incipit">

        <marc:datafield tag="246" ind1="3" ind2=" ">
            <marc:subfield code="i">Incipit: </marc:subfield>
            <marc:subfield code="a">
                <xsl:apply-templates select="tei:locus"/>
                <xsl:text> </xsl:text>
                <xsl:apply-templates select="text()"/>
                
                <!--<xsl:value-of select="normalize-space(.)"/>-->
            </marc:subfield>
        </marc:datafield>

    </xsl:template>

   <!--
 <xsl:template match="tei:explicit">
        
        <marc:datafield tag="246" ind1="3" ind2=" ">
            <marc:subfield code="i">Explicit: </marc:subfield>
            <marc:subfield code="a">
                <xsl:apply-templates select="tei:locus"/>
                <xsl:text> </xsl:text>
                <xsl:apply-templates select="text()"/>
                
            </marc:subfield>
        </marc:datafield>
        
    </xsl:template>
   -->

    <xsl:template match="tei:note">
        
        <marc:datafield tag="500" ind1=" " ind2=" ">
            <marc:subfield code="a"><xsl:value-of select="normalize-space(.)"/></marc:subfield>
        </marc:datafield>
        
    </xsl:template>
    
    <xsl:template match="tei:physDesc">
        
        <xsl:apply-templates select="tei:objectDesc"/>
        
       <!--  
	   <xsl:apply-templates select="tei:handDesc"/>
	   -->
        
        <!--
		<xsl:apply-templates select="tei:additions"/>
        -->
		
       <xsl:apply-templates select="tei:accMat"/>
        
    </xsl:template>
    
    <!--all the stuff to do with objectDesc-->
    <xsl:template match="tei:objectDesc">
        
        <marc:datafield tag="500" ind1=" " ind2=" ">
            <marc:subfield code="a">Form:<xsl:text> </xsl:text><xsl:value-of select="normalize-space(@form)"/>.</marc:subfield>
        </marc:datafield>
        
        <xsl:apply-templates select="tei:supportDesc"/>
        
    </xsl:template>

    <xsl:template match="tei:supportDesc">
        
        <marc:datafield tag="500" ind1=" " ind2=" ">
            <marc:subfield code="a">Material:<xsl:text> </xsl:text><xsl:value-of select="normalize-space(@material)"/>.</marc:subfield>
        </marc:datafield>
        
        
        <!--
		
		<xsl:apply-templates select="tei:extent"/>
    
        <xsl:apply-templates select="tei:foliation"/>
    
        <xsl:apply-templates select="tei:collation"/>
		
		-->
        
        
    </xsl:template>

    <xsl:template match="tei:extent">
		
		<marc:datafield tag="300" ind1=" " ind2=" ">
            <marc:subfield code="a"><xsl:value-of select="normalize-space(text())"/></marc:subfield>
            <marc:subfield code="c"><xsl:apply-templates select="tei:dimensions[@type='leaf']"/></marc:subfield>
        </marc:datafield>
        
    </xsl:template>
    
    <xsl:template match="tei:dimensions[@type='leaf']">
        
        <xsl:apply-templates select="tei:height"/><xsl:text> x </xsl:text><xsl:apply-templates select="tei:width"/><xsl:text> mm</xsl:text>
        
    </xsl:template>

    <xsl:template match="tei:foliation">
        <marc:datafield tag="500" ind1=" " ind2=" ">
            <marc:subfield code="a"><xsl:text>Foliation: </xsl:text><xsl:value-of select="normalize-space(.)"/></marc:subfield>
        </marc:datafield>
    </xsl:template>

    <xsl:template match="tei:collation">
        <marc:datafield tag="500" ind1=" " ind2=" ">
            <marc:subfield code="a"><xsl:text>Collation: </xsl:text><xsl:value-of select="normalize-space(.)"/></marc:subfield>
        </marc:datafield>
    </xsl:template>
    
    <!--handDesc stuff-->
    <xsl:template match="tei:handDesc">
        
        <xsl:apply-templates select="tei:handNote"/>
        
    </xsl:template>
    
    <xsl:template match="tei:handNote">
        
        <marc:datafield tag="500" ind1=" " ind2=" ">
            <marc:subfield code="a"><xsl:text>Script: </xsl:text><xsl:value-of select="normalize-space(@script)"/>.</marc:subfield>
        </marc:datafield>
    
        <xsl:apply-templates select="tei:seg[@type='ink']"></xsl:apply-templates>
        
        <xsl:apply-templates select="tei:seg/tei:persName[@type='standard' and @xml:lang='ara-Latn']"></xsl:apply-templates>
    
    </xsl:template>

    <xsl:template match="tei:seg[@type='ink']">
        
        <marc:datafield tag="500" ind1=" " ind2=" ">
            <marc:subfield code="a"><xsl:text>Ink: </xsl:text><xsl:value-of select="normalize-space(.)"/>.</marc:subfield>
        </marc:datafield>
        
    </xsl:template>
    
    <xsl:template match="tei:seg/tei:persName[@type='standard' and @xml:lang='ara-Latn']">
        
        <marc:datafield tag="700" ind1="0" ind2=" ">                
            <marc:subfield code="a"><xsl:value-of select="normalize-space(.)"/></marc:subfield>
            <marc:subfield code="e"><xsl:text>scribe.</xsl:text></marc:subfield>
        </marc:datafield>
        
    </xsl:template>
    
    <!--additions stuff-->
    <xsl:template match="tei:additions">
        
        <xsl:apply-templates select="tei:list/tei:item"/>
            
    </xsl:template>
    
    <xsl:template match="tei:list/tei:item">
        
        <marc:datafield tag="500" ind1=" " ind2=" ">
            <marc:subfield code="a">
                <xsl:apply-templates select="tei:label"/>
                <xsl:value-of select="normalize-space(text())"/>
            </marc:subfield>
        </marc:datafield>
        
    </xsl:template>
    
    <xsl:template match="tei:label">
        
        <xsl:value-of select="normalize-space(.)"/><xsl:text>: </xsl:text>
        
    </xsl:template>
    
    <!--accMat stuff-->
    
    <xsl:template match="tei:accMat">
        
        <xsl:apply-templates select="tei:p"/>
            
    </xsl:template>

    <xsl:template match="tei:p">
        
        <marc:datafield tag="500" ind1=" " ind2=" ">
            <marc:subfield code="a">
                <xsl:value-of select="normalize-space(.)"/>
            </marc:subfield>
        </marc:datafield>
        
    </xsl:template>
    
   <!--history stuff-->
    
    <xsl:template match="tei:history">
        <xsl:apply-templates select="tei:origin"/>
        <xsl:apply-templates select="tei:acquisition"/>
        <xsl:apply-templates select="tei:provenance"/>
    </xsl:template>

    <!--origin stuff-->
    <xsl:template match="tei:origin">
        <xsl:apply-templates select="tei:date"/>
    </xsl:template>

    <xsl:template match="tei:date">
        <marc:datafield tag="260" ind1=" " ind2=" ">
            <marc:subfield code="c">
                <xsl:value-of select="normalize-space(.)"/>
            </marc:subfield>
        </marc:datafield>
    </xsl:template>
    
    <!--provenance stuff-->
    <xsl:template match="tei:provenance">
        <marc:datafield tag="561" ind1=" " ind2=" ">
            <marc:subfield code="a">
                <xsl:if test="tei:locus">
                    <xsl:apply-templates select="tei:locus"/>
                    <xsl:text> </xsl:text>    
                </xsl:if>       
                <xsl:apply-templates select="text()"/>
            </marc:subfield>
        </marc:datafield>
        
    </xsl:template>

    <!--acquisition stuff-->
    <xsl:template match="tei:acquisition">
        <marc:datafield tag="541" ind1=" " ind2=" ">
            <marc:subfield code="a">
                <xsl:value-of select="normalize-space(.)"/>
            </marc:subfield>
        </marc:datafield>
        
    </xsl:template>
    
    <!--classmark-->
   <xsl:template match="tei:idno">
         <marc:datafield tag="856" ind1=" " ind2=" ">
            <marc:subfield code="u">http://wamcp.bibalex.org/viewfull?Shelfmark=<xsl:value-of select="translate(normalize-space(.),' ','_')"/></marc:subfield>
        </marc:datafield>
	<!--Michael: Title-->
   <marc:datafield tag="699" ind1=" " ind2=" ">
		<marc:subfield code="a"> 
			<xsl:value-of select="normalize-space(.)"/>
		</marc:subfield>
	</marc:datafield>
    </xsl:template>
	
	    <!--Michael: language-->
    <xsl:template match="tei:textLang">
        <marc:datafield tag="040" ind1=" " ind2=" ">
            <marc:subfield code="b">
                <!--<xsl:value-of select="normalize-space(.)"/> -->				
				<xsl:value-of select="substring(@mainLang,0,4)"/>
            </marc:subfield>
        </marc:datafield>
    </xsl:template>	
</xsl:stylesheet>
