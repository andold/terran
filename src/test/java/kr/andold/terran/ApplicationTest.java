package kr.andold.terran;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.junit.jupiter.api.Test;

import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApplicationTest {
	private static final String TAB_MARK = "\t";
	private static final String NEWLINE_MARK = "\n";

	@Test
	public void testMain() throws IOException {
		testExcel("src/test/resources/주식종목전체검색.xlsx");
	}

	@Test
	public void testDivident() throws IOException {
		FileInputStream file = new FileInputStream("src/test/resources/배당내역상세.xls");
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(file, "euc-kr"));
		String html = extractStringFromText(bufferedReader);
		Document document = Jsoup.parse(html);
		String stringBuffer = extractText((Node)document);
		String string = stringBuffer.toString();
		log.info("{}", string);
	}

	private static String extractStringFromText(BufferedReader bufferedReader) {
		try {
			String line = null;
			StringBuffer stringBuffer = new StringBuffer();
			int lineno = 0;
			while ((line = bufferedReader.readLine()) != null) {
				stringBuffer.append(line);
				stringBuffer.append("\n");
				if (lineno++ % 1024 == 0) {
					log.debug("{} {}:	{}", Utility.indentMiddle(), lineno, line);
				}
			}
			bufferedReader.close();

			return new String(stringBuffer);
		} catch (Exception e) {
			log.warn("{} Exception:: {}", Utility.indentMiddle(), e.getMessage(), e);
		}

		return null;
	}

	private static String extractText(Node node) {
		if (node instanceof TextNode) {
			return ((TextNode)node).text();
		}

		StringBuffer stringBuffer = new StringBuffer();
		String nodeName = node.nodeName();

		switch (nodeName) {
		case "tr":
			node.childNodes().forEach(child -> {
				stringBuffer.append(extractText(child));
			});
			stringBuffer.append(NEWLINE_MARK);
			break;
		case "td":
		case "th":
			StringBuffer sb = new StringBuffer();
			node.childNodes().forEach(child -> {
				sb.append(extractText(child));
			});
			stringBuffer.append(sb);
			stringBuffer.append(TAB_MARK);
			break;
		default:
			node.childNodes().forEach(child -> {
				stringBuffer.append(extractText(child));
			});
			break;
		}

		return stringBuffer.toString();
	}

	private void testExcel(String filename) throws IOException {
		FileInputStream file = new FileInputStream(filename);

		// Create Workbook instance holding reference to .xlsx file
		Workbook workbook = null;

		if (filename.endsWith("xlsx")) {
			workbook = new XSSFWorkbook(file);
		} else if (filename.endsWith("xls")) {
			workbook = new HSSFWorkbook(file);
		}
		
		// Get first/desired sheet from the workbook
		Sheet sheet = workbook.getSheetAt(0);

		// Iterate through each rows one by one
		Iterator<Row> rowIterator = sheet.iterator();
		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();
			// For each row, iterate through all the columns
			Iterator<Cell> cellIterator = row.cellIterator();

			while (cellIterator.hasNext()) {
				Cell cell = cellIterator.next();
				// Check the cell type and format accordingly
				switch (cell.getCellType()) {
				case NUMERIC:
					System.out.print(cell.getNumericCellValue() + "\t");
					break;
				case STRING:
					System.out.print(cell.getStringCellValue() + "\t");
					break;
				default:
					throw new IllegalStateException("Unexpected value: " + cell.getCellType());
				}
			}
			System.out.println("");
		}
		file.close();
	}

}
