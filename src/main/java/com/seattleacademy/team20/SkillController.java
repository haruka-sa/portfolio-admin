package com.seattleacademy.team20;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

@Controller
public class SkillController {

  private static final Logger logger = LoggerFactory.getLogger(SkillController.class);

  @Autowired
  //MySQLのデータを取ってくるためのライブラリ
  private JdbcTemplate jdbcTemplate;

  @RequestMapping(value = "/skillUpload", method = RequestMethod.GET)
  public String skillUpload(Locale locale, Model model) throws IOException {
    logger.info("Welcome home! The client locale is {}.", locale);

    Date date = new Date();
    DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale);

    String formattedDate = dateFormat.format(date);

    model.addAttribute("serverTime", formattedDate);

    initialize();
    List<SkillCategory> skills = selectSkills();
    uploadSkill(skills);

    return "skillUpload";
  }

  //Listの宣言
  public List<SkillCategory> selectSkills() {
    //sequel proのテーブルからデータを取得する
    // 上書きしないのでfinal
    final String sql = "select * from skills";
    //jdbcTemplateでsqlを実行
    return jdbcTemplate.query(sql, new RowMapper<SkillCategory>() {
      //呪文
      public SkillCategory mapRow(ResultSet rs, int rowNum) throws SQLException {
        // SkillCategoryの中にそれぞれのデータを入れている　その後にRowMapper<Skillcategory>に返却される
        return new SkillCategory(rs.getInt("id"), rs.getString("category"),
            rs.getString("name"), rs.getInt("score"));
      }
    });
  }

  private FirebaseApp app;

  // SDKの初期化
  public void initialize() throws IOException {
    FileInputStream refreshToken = new FileInputStream(
        "/Users/saitoharuka/Downloads/dev-portfolio-d25e3-firebase-adminsdk-vizo5-d976da9d63-copy.json");
    FirebaseOptions options = new FirebaseOptions.Builder()
        .setCredentials(GoogleCredentials.fromStream(refreshToken))
        .setDatabaseUrl("https://dev-portfolio-d25e3.firebaseio.com/")
        .build();
    app = FirebaseApp.initializeApp(options, "other");
  }

  public void uploadSkill(List<SkillCategory> skills) {
    // データの保存
    final FirebaseDatabase database = FirebaseDatabase.getInstance(app);
    DatabaseReference ref = database.getReference("skillCategories");

    // DatabaseReference skillRef = ref.child("skillcategories");

    //Map型のリストを作る。MapはStringで聞かれたものに対し、Object型で返すようにしている
    List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
    Map<String, Object> map;
    Map<String, List<SkillCategory>> skillMap = skills.stream()
        .collect(Collectors.groupingBy(SkillCategory::getCategory));
    for (Map.Entry<String, List<SkillCategory>> entry : skillMap.entrySet()) {
      map = new HashMap<>();
      map.put("category", entry.getKey());
      map.put("skills", entry.getValue());

      dataList.add(map);
    }
    //skillsRef.updateChildrenAsync(dataMap);
    ref.setValue(dataList, new DatabaseReference.CompletionListener() {
      @Override
      public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
        if (databaseError != null) {
          System.out.println("Data could be saved" + databaseError.getMessage());
        } else {
          System.out.println("Data save successfully");
        }
      }
    });
  }
}