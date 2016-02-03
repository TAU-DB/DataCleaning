package data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class RulesReader {

	private Document m_doc;
	private List<Rule> m_rules;
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	public RulesReader(String rulesFile) {
		try {
			LOGGER.info("Parsing rules file: " + rulesFile);
			File inputFile = new File(rulesFile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder;

			dBuilder = dbFactory.newDocumentBuilder();

			m_doc = dBuilder.parse(inputFile);
			m_doc.getDocumentElement().normalize();

			XPath xPath = XPathFactory.newInstance().newXPath();

			String expression = "//rule";
			NodeList ruleNodeList = (NodeList) xPath.compile(expression).evaluate(m_doc, XPathConstants.NODESET);
			m_rules = new ArrayList<Rule>();
			for (int i = 0; i < ruleNodeList.getLength(); i++) {
				Element ruleElm = (Element) ruleNodeList.item(i);
				Rule rule = parseRule(ruleElm);
				m_rules.add(rule);
			}

			String rulesStr = "";
			for (Rule rule : m_rules) {
				rulesStr += rule.toString() + System.lineSeparator();
				rulesStr += "=======================================" + System.lineSeparator();
			}

			LOGGER.info("Detected the rules: " + System.lineSeparator() + rulesStr);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

	public List<Rule> getRules() {
		return m_rules;
	}

	private Rule parseRule(Element ruleElm) {

		String type = ruleElm.getAttribute("type");
		try {
			XPath xPath = XPathFactory.newInstance().newXPath();

			String expression = "./false_query";
			NodeList queryNodeList = (NodeList) xPath.compile(expression).evaluate(ruleElm, XPathConstants.NODESET);
			String falseQuery = queryNodeList.item(0).getTextContent();

			expression = "./lhs/*";
			NodeList lhsFormulasNodeList = (NodeList) xPath.compile(expression).evaluate(ruleElm,
					XPathConstants.NODESET);
			List<Formula> lhs = new ArrayList<Formula>();
			for (int i = 0; i < lhsFormulasNodeList.getLength(); i++) {
				Element formulaElm = (Element) lhsFormulasNodeList.item(i);
				Formula formula = null;
				if (formulaElm.getTagName().equals("relational_formula")) {
					formula = parseRelationalFormula(formulaElm);
				} else {
					formula = parseConditionalFormula(formulaElm);
				}
				lhs.add(formula);
			}
			
			expression = "./rhs/*";
			NodeList rhsFormulasNodeList = (NodeList) xPath.compile(expression).evaluate(ruleElm,
					XPathConstants.NODESET);
			List<Formula> rhs = new ArrayList<Formula>();
			for (int i = 0; i < rhsFormulasNodeList.getLength(); i++) {
				Element formulaElm = (Element) rhsFormulasNodeList.item(i);
				Formula formula = null;
				if (formulaElm.getTagName().equals("relational_formula")) {
					formula = parseRelationalFormula(formulaElm);
				} else {
					formula = parseConditionalFormula(formulaElm);
				}
				rhs.add(formula);
			}			
			
			return new Rule(type, falseQuery, lhs, rhs);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		return null;
	}

	private RelationalFormula parseRelationalFormula(Element formulaElm) {

		int index = Integer.parseInt(formulaElm.getAttribute("index"));
		String table = formulaElm.getAttribute("table");
		List<Variable> variables = parseFormulaVariables(formulaElm);
		return new RelationalFormula(index, table, variables);
	}

	private ConditionalFormula parseConditionalFormula(Element formulaElm) {

		int index = Integer.parseInt(formulaElm.getAttribute("index"));
		String operator = formulaElm.getAttribute("operator");
		List<Variable> variables = parseFormulaVariables(formulaElm);
		return new ConditionalFormula(index, variables, operator);
	}

	private List<Variable> parseFormulaVariables(Element formulaElm) {
		try {
			XPath xPath = XPathFactory.newInstance().newXPath();

			String expression = "./variable";
			NodeList variablesNodeList = (NodeList) xPath.compile(expression).evaluate(formulaElm,
					XPathConstants.NODESET);
			List<Variable> variables = new ArrayList<Variable>();

			for (int i = 0; i < variablesNodeList.getLength(); i++) {
				Element variableElm = (Element) variablesNodeList.item(i);
				String name = variableElm.getAttribute("name");
				String column = variableElm.getAttribute("column");
				String isConstantStr = variableElm.getAttribute("is_constant");
				boolean isConstant = false;
				if (isConstantStr.toLowerCase().equals("true")) {
					isConstant = true;
				}
				String value = variableElm.getAttribute("value");
				Variable var = new Variable(name, column, isConstant, value);
				variables.add(var);
			}

			return variables;
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		return new ArrayList<Variable>();
	}
}
