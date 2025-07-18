object XSD2RMLGenerator {
  def generateRML(xsdPath: String, outputPath: String): Unit = {
    val types = XSD2X3MLGenerator.parseXSD(xsdPath)

    val sb = new StringBuilder
    sb.append("""
      @prefix rr: <http://www.w3.org/ns/r2rml#> .
      @prefix rml: <http://semweb.mmlab.be/ns/rml#> .
      @prefix ql: <http://semweb.mmlab.be/ns/ql#> .
      @prefix ex: <https://your-namespace/> .
      @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

    """)

    types.foreach {
      case ct if ct.kind == "node" =>
        val tmName = s"ex:${ct.name}Mapping"
        sb.append(s"$tmName a rr:TriplesMap ;\n")
        sb.append(s"""  rml:logicalSource [ rml:source "input.xml" ; rml:referenceFormulation ql:XPath ; rml:iterator "//${ct.name}" ] ;\n""")
        sb.append(s"""  rr:subjectMap [ rr:template "https://your-namespace/${ct.name}/{id}" ; rr:class ex:${ct.name} ] ;\n""")

        ct.fields.foreach { case (fname, _) =>
          sb.append(s"""  rr:predicateObjectMap [ rr:predicate ex:has_${fname} ; rr:objectMap [ rml:reference "${fname}" ; ] ] ;\n""")
        }

        sb.append(".\n\n")

      case ct if ct.kind == "edge" =>
        val tmName = s"ex:${ct.name}Mapping"
        sb.append(s"$tmName a rr:TriplesMap ;\n")
        sb.append(s"""  rml:logicalSource [ rml:source "input.xml" ; rml:referenceFormulation ql:XPath ; rml:iterator "//${ct.name}" ] ;\n""")

        val sourceType = ct.fields.find(_._1 == "source").map(_._2).getOrElse("UnknownSource")
        val targetType = ct.fields.find(_._1 == "target").map(_._2).getOrElse("UnknownTarget")

        sb.append(s"""  rr:subjectMap [ rr:template "https://your-namespace/${ct.name}/{id}" ; rr:class ex:${ct.name} ] ;\n""")

        // Source -> Intermediate -> Target
        sb.append(s"""  rr:predicateObjectMap [ rr:predicate ex:source ; rr:objectMap [ rr:template "https://your-namespace/${sourceType}/{source/${sourceType}}" ] ] ;\n""")
        sb.append(s"""  rr:predicateObjectMap [ rr:predicate ex:target ; rr:objectMap [ rr:template "https://your-namespace/${targetType}/{target/${targetType}}" ] ] ;\n""")

        ct.fields.filterNot(f => f._1 == "source" || f._1 == "target").foreach { case (fname, _) =>
          sb.append(s"""  rr:predicateObjectMap [ rr:predicate ex:${ct.name}_${fname} ; rr:objectMap [ rml:reference "${fname}" ] ] ;\n""")
        }

        sb.append(".\n\n")

      case _ => ()
    }

    import java.io.PrintWriter
    val out = new PrintWriter(outputPath)
    out.write(sb.toString())
    out.close()
    println(s"[DONE] RML mapping generated to $outputPath")
  }
}
