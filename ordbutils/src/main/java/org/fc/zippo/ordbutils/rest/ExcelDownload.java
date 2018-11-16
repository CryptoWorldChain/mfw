package org.fc.zippo.ordbutils.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderFormatting;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.ComparisonOperator;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.fc.zippo.ordbutils.bean.DbCondi;
import org.fc.zippo.ordbutils.exception.DirectOutputStreamException;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.ojpa.ordb.loader.CommonSqlMapper;
import onight.tfw.outils.serialize.JsonSerializer;

@Slf4j
public class ExcelDownload extends DirectOutputStreamException {

	HttpServletRequest req;

	DbCondi dc;

	int totalCount;

	protected CommonSqlMapper mapper;

	HashMap<String, HashMap<String, String>> emap = new HashMap<>();

	public ExcelDownload(HttpServletRequest req, DbCondi dc, int totalCount, CommonSqlMapper mapper) {
		super();
		this.req = req;
		this.dc = dc;
		this.totalCount = totalCount;
		this.mapper = mapper;
	}

	@Override
	public void doReponse(HttpServletResponse res) {
		Workbook wb = new HSSFWorkbook(); // or new XSSFWorkbook();
		try {
			res.setHeader("Content-type", "application/vnd.ms-excel");
			OutputStream out = res.getOutputStream();
			String emtitle = req.getParameter("et");

			String fileName = URLEncoder.encode(emtitle, "UTF-8");
			fileName = URLDecoder.decode(fileName, "ISO8859_1");

			res.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + ".xls\"");

			int pageSize = dc.getPageinfo().getLimit();
			String nameCols[] = req.getParameter("ncol").split(",");
			String dbCols[] = req.getParameter("dbcol").split(",");

			HashMap<String, String> coltitle = new HashMap<>();

			if (req.getParameter("ci") != null) {
				coltitle = JsonSerializer.getInstance().deserialize(req.getParameter("ci"), coltitle.getClass());

			}
			if (req.getParameter("remap") != null) {
				HashMap<String, String> remap = JsonSerializer.getInstance().deserialize(req.getParameter("remap"), coltitle.getClass());
				for (Map.Entry<String, String> em : remap.entrySet()) {
					HashMap<String, String> map = new HashMap<>();
					if (em.getValue().startsWith("db@")) {// tb,key,display
						String dbinfo[] = em.getValue().substring(3).split(",");
						if (dbinfo.length >= 3) {
							String tb = dbinfo[0];
							String key = dbinfo[1];
							String display = dbinfo[2];
							String where = "";
							if (dbinfo.length > 3)
								where = " WHERE " + dbinfo[3];
							List<Map<String, Object>> rt = mapper
									.executeSql("SELECT " + FieldUtils.field2SqlColomn(key) + "," + FieldUtils.field2SqlColomn(display) + " FROM " + tb + where);
							for (Map<String, Object> dbmap : rt) {// map
								String ekey = "" + dbmap.get(FieldUtils.field2SqlColomn(key));
								String evalue = "" + dbmap.get(FieldUtils.field2SqlColomn(display));
								map.put(ekey, evalue);
							}
							emap.put(em.getKey(), map);
						}
					} else {
						for (String kv : em.getValue().split(",")) {
							String kvs[] = kv.split(":");
							if (kvs.length == 2) {
								map.put(kvs[0], kvs[1]);
							}
						}
						emap.put(em.getKey(), map);
					}

				}

			}

			int pageId = 0;
			if (totalCount == 0) {
				fetchPage(wb, emtitle, pageSize, nameCols, dbCols, 0, pageId, coltitle);
			} else {
				for (int i = 0; i < totalCount; i += pageSize) {
					fetchPage(wb, emtitle, pageSize, nameCols, dbCols, i, pageId, coltitle);
					pageId++;
				}
			}

			wb.write(out);
			
		} catch (Exception e) {
			try {
				log.warn("下载excel失败",e);
				res.sendError(HttpServletResponse.SC_BAD_REQUEST, "导出失败:" + e.getMessage());
			} catch (IOException e1) {
			}
		}finally{
			try {
				if(wb!=null){
					wb.close();
				}
			} catch (IOException e) {
			}
		}

	}

	public void writeHeader(Sheet sheet, Workbook wb, String emtitle, int pageSize, String nameCols[], String dbCols[], int skip, int pageId, HashMap<String, String> coltitle) {
		// 写入前面两行
		Row row0 = sheet.createRow(0);
		Row row1 = sheet.createRow(1);
		row0.setHeightInPoints(30);
		row1.setHeightInPoints(30);
		Cell tcell = row0.createCell(0);
		tcell.setCellValue(emtitle);
		if (headstyle == null) {
			Font tfont = wb.createFont();
			tfont.setFontHeightInPoints((short) 24);
			// tfont.setFontName("Courier New");
			CellStyle tstyle = wb.createCellStyle();
			tstyle.setFont(tfont);
			tstyle.setAlignment(HorizontalAlignment.CENTER);
			tstyle.setVerticalAlignment(VerticalAlignment.CENTER);
			headstyle = tstyle;
		}
		tcell.setCellStyle(headstyle);

		sheet.addMergedRegion(new CellRangeAddress(0, // first row (0-based)
				1, // last row (0-based)
				0, // first column (0-based)
				dbCols.length // last column (0-based)
		));
		Row row2 = sheet.createRow(2);
		Cell celldate = row2.createCell(0);
		celldate.setCellValue(new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(new Date()));

		if (subheadstyle == null) {
			Font tfont = wb.createFont();
			tfont.setFontHeightInPoints((short) 16);
			// tfont.setFontName("Courier New");
			// Fonts are set into a style so create a new one to use.
			CellStyle tstyle = wb.createCellStyle();
			tstyle.setFont(tfont);
			tstyle.setAlignment(HorizontalAlignment.CENTER);
			tstyle.setVerticalAlignment(VerticalAlignment.CENTER);
			subheadstyle = tstyle;
			// Create a cell and put a value in it.
		}
		celldate.setCellStyle(subheadstyle);

		sheet.addMergedRegion(new CellRangeAddress(2, // first row (0-based)
				2, // last row (0-based)
				0, // first column (0-based)
				dbCols.length // last column (0-based)
		));

		Row row3 = sheet.createRow(3);
		Cell hcellno = row3.createCell(0);
		hcellno.setCellValue("序号");

		for (int i = 0; i < nameCols.length; i++) {
			Cell cell = row3.createCell(i + 1);
			cell.setCellValue(nameCols[i]);
			{
				if (columncellstyle == null) {
					Font tfont = wb.createFont();
					tfont.setFontHeightInPoints((short) 15);
					// tfont.setFontName("Courier New");
					CellStyle tstyle = wb.createCellStyle();
					tstyle.setFont(tfont);
					columncellstyle = tstyle;
				}
				cell.setCellStyle(columncellstyle);
				if (coltitle.containsKey(dbCols[i])) {
					setCellComments(wb, cell, coltitle.get(dbCols[i]));
				}
				hcellno.setCellStyle(columncellstyle);
			}
		}

	}

	CellStyle columncellstyle;
	CellStyle subheadstyle;
	CellStyle headstyle;

	public void setCellComments(Workbook wb, Cell cell, String strcom) {

		CreationHelper factory = wb.getCreationHelper();

		ClientAnchor anchor = factory.createClientAnchor();
		anchor.setCol1(cell.getColumnIndex());
		anchor.setCol2(cell.getColumnIndex() + 3);
		anchor.setRow1(cell.getRow().getRowNum());
		anchor.setRow2(cell.getRow().getRowNum() + 2);

		Drawing drawing = cell.getSheet().createDrawingPatriarch();
		// Create the comment and set the text+author
		Comment comment = drawing.createCellComment(anchor);
		RichTextString str = factory.createRichTextString(strcom);
		comment.setString(str);
		comment.setAuthor("BREW");

		// Assign the comment to the cell
		cell.setCellComment(comment);
	}

	CellStyle cellstyle;
	CellStyle sumstyle;

	public void fetchPage(Workbook wb, String emtitle, int pageSize, String nameCols[], String dbCols[], int skip, int pageId, HashMap<String, String> coltitle) {
		Sheet sheet = wb.createSheet("第" + (pageId + 1) + "页");
		writeHeader(sheet, wb, emtitle, pageSize, nameCols, dbCols, skip, pageId, coltitle);
		// do for each page!
		dc.getPageinfo().setSkip(skip);
		String sql = SqlMaker.getSQL(dc);
		log.debug("[SQL]:{}", sql);
		List<Map<String, Object>> list = mapper.executeSql(sql);
		SimpleDateFormat sdfdate = new SimpleDateFormat("YYYY-MM-dd");
		SimpleDateFormat sdftime = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
		if (list != null && list.size() > 0) {
			list = FieldUtils.reMap(list);
			for (int dbrowid = 0; dbrowid < list.size(); dbrowid++) {
				Row row = sheet.createRow(dbrowid + 4);
				Map<String, Object> dbrow = list.get(dbrowid);
				for (int ci = 0; ci < dbCols.length; ci++) {
					String dbcol = dbCols[ci];
					Cell hcellno = row.createCell(0);
					hcellno.setCellValue(dbrowid + 1);
					Cell cell = row.createCell(ci + 1);
					Object dbobj = dbrow.get(dbcol);
					if (emap.containsKey(dbcol)) {
						Map<String, String> map = emap.get(dbcol);
						if (map.containsKey(dbobj)) {
							dbobj = map.get(dbobj);
						}
					}
					{
						if (cellstyle == null) {
							Font tfont = wb.createFont();
							tfont.setFontHeightInPoints((short) 16);
							// tfont.setFontName("Courier New");
							CellStyle tstyle = wb.createCellStyle();
							tstyle.setFont(tfont);
							cellstyle = tstyle;
						}

						cell.setCellStyle(cellstyle);
						hcellno.setCellStyle(cellstyle);
					}

					if (dbobj instanceof String) {
						cell.setCellType(CellType.STRING);
						cell.setCellValue(String.valueOf(dbobj));
					} else if (dbobj instanceof Date) {
						cell.setCellType(CellType.STRING);
						cell.setCellValue(sdfdate.format((Date) dbobj));
					} else if (dbobj instanceof Timestamp) {
						cell.setCellType(CellType.STRING);
						cell.setCellValue(sdftime.format((Timestamp) dbobj));
					} else if (dbobj instanceof BigDecimal) {
						cell.setCellType(CellType.NUMERIC);
						cell.setCellValue(((BigDecimal) dbobj).doubleValue());
					} else if (dbobj instanceof Long) {
						cell.setCellType(CellType.NUMERIC);
						cell.setCellValue(((Long) dbobj));
					} else if (dbobj instanceof Integer) {
						cell.setCellValue(((Integer) dbobj));
						cell.setCellType(CellType.NUMERIC);
					} else if (dbobj != null) {
						cell.setCellValue("" + dbobj);
						cell.setCellType(CellType.STRING);
					} else {
						cell.setCellValue("");
						cell.setCellType(CellType.STRING);
					}

				}
			}

		}
		Row row = sheet.createRow(list.size() + 4);
		// total

		Cell celltail = row.createCell(0);
		celltail.setCellValue("总数:");

		if (sumstyle == null) {
			Font tfont = wb.createFont();
			tfont.setFontHeightInPoints((short) 16);
			CellStyle tstyle = wb.createCellStyle();
			tstyle.setFont(tfont);
			tstyle.setAlignment(HorizontalAlignment.CENTER);
			tstyle.setVerticalAlignment(VerticalAlignment.CENTER);
			sumstyle = tstyle;
		}
		celltail.setCellStyle(sumstyle);
		sheet.addMergedRegion(new CellRangeAddress(list.size() + 4, // first row
																	// (0-based)
				list.size() + 4, // last row (0-based)
				0, // first column (0-based)
				dbCols.length - 1 // last column (0-based)
		));
		Cell cccell = row.createCell(dbCols.length);
		cccell.setCellValue(list.size());

		cccell.setCellStyle(sumstyle);

		for (int i = 0; i < nameCols.length + 1; i++) {
			sheet.autoSizeColumn(i);
		}

		SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();

		ConditionalFormattingRule rule1 = sheetCF.createConditionalFormattingRule(ComparisonOperator.NOT_EQUAL, "111111110");

		BorderFormatting bordFmt = rule1.createBorderFormatting();
		bordFmt.setBorderBottom(BorderStyle.THIN);
		bordFmt.setBorderTop(BorderStyle.THIN);
		bordFmt.setBorderLeft(BorderStyle.THIN);
		bordFmt.setBorderRight(BorderStyle.THIN);

		PatternFormatting patternFmt = rule1.createPatternFormatting();

		ConditionalFormattingRule[] cfRules = { rule1 };

		char a = (char) ('A' + nameCols.length);
		CellRangeAddress[] regions = { CellRangeAddress.valueOf("A1:" + a + (list.size() + 5)) };

		sheetCF.addConditionalFormatting(regions, cfRules);
	}

}
