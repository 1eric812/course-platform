package com.courseplatform.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 课程平台数据访问层：全部基于注解 SQL，字段别名保持原 Node 版接口的 camelCase 契约。
 * 表结构见 resources/db/schema.sql（16 张表）。
 */
@Mapper
public interface CoreMapper {

    /* ==================== 账号 / 档案 ==================== */

    @Select("SELECT id, username, password, role, real_name AS realName, status "
            + "FROM sys_user WHERE username = #{username} AND deleted = 0 LIMIT 1")
    Map<String, Object> findUserByUsername(@Param("username") String username);

    @Select("SELECT id, role, real_name AS realName, username FROM sys_user WHERE id = #{id}")
    Map<String, Object> findUserById(@Param("id") long id);

    @Update("UPDATE sys_user SET last_login_at = NOW() WHERE id = #{id}")
    int touchLogin(@Param("id") long id);

    @Select("SELECT id AS studentId, student_no AS studentNo, grade, major, credit_limit AS creditLimit "
            + "FROM student WHERE user_id = #{userId} AND deleted = 0 LIMIT 1")
    Map<String, Object> findStudentByUserId(@Param("userId") long userId);

    @Select("SELECT s.id AS studentId, s.student_no AS studentNo, u.real_name AS name, s.grade, s.major, "
            + "s.credit_limit AS creditLimit FROM student s JOIN sys_user u ON u.id = s.user_id "
            + "WHERE s.user_id = #{userId} AND s.deleted = 0 LIMIT 1")
    Map<String, Object> studentProfileByUserId(@Param("userId") long userId);

    @Select("SELECT t.id AS teacherId, t.teacher_no AS teacherNo, t.title, t.dept, u.real_name AS name "
            + "FROM teacher t JOIN sys_user u ON u.id = t.user_id "
            + "WHERE t.user_id = #{userId} AND t.deleted = 0 LIMIT 1")
    Map<String, Object> findTeacherByUserId(@Param("userId") long userId);

    @Select("SELECT COUNT(*) FROM student WHERE deleted = 0")
    long countStudents();

    /* ==================== 课程 ==================== */

    @Select("SELECT id, code, name, category, credits, teacher_id AS teacherId, teacher_name AS teacher, "
            + "campus, place, assessment, capacity, enrolled, remaining, rating, prereq_name AS prereq, tags, intro "
            + "FROM course WHERE deleted = 0 ORDER BY id")
    List<Map<String, Object>> allCourses();

    @Select("SELECT id, code, name, category, credits, teacher_id AS teacherId, teacher_name AS teacher, "
            + "campus, place, assessment, capacity, enrolled, remaining, rating, prereq_name AS prereq, tags, intro "
            + "FROM course WHERE id = #{id} AND deleted = 0")
    Map<String, Object> courseById(@Param("id") long id);

    @Select("SELECT course_id AS courseId, day_of_week AS day, start_period AS start, end_period AS end "
            + "FROM course_schedule ORDER BY course_id, day_of_week, start_period")
    List<Map<String, Object>> allSchedules();

    @Select("SELECT course_id AS courseId, prereq_name AS prereqName FROM course_prerequisite")
    List<Map<String, Object>> allPrereqs();

    @Update("UPDATE course SET enrolled = enrolled + 1 WHERE id = #{id} AND enrolled < capacity AND deleted = 0")
    int takeSeat(@Param("id") long id);

    @Update("UPDATE course SET enrolled = GREATEST(enrolled - #{n}, 0) WHERE id = #{id}")
    int releaseSeat(@Param("id") long id, @Param("n") int n);

    @Update("UPDATE course SET capacity = GREATEST(capacity + #{delta}, enrolled) WHERE id = #{id} AND deleted = 0")
    int adjustCapacity(@Param("id") long id, @Param("delta") int delta);

    @Update("UPDATE course SET place = #{place}, intro = #{intro} WHERE id = #{id} AND deleted = 0")
    int updateCourseInfo(@Param("id") long id, @Param("place") String place, @Param("intro") String intro);

    @Select("SELECT COUNT(*) FROM course WHERE deleted = 0")
    long countCourses();

    @Select("SELECT IFNULL(SUM(capacity), 0) FROM course WHERE deleted = 0")
    long sumCapacity();

    @Select("SELECT IFNULL(SUM(enrolled), 0) FROM course WHERE deleted = 0")
    long sumEnrolled();

    @Select("SELECT COUNT(*) FROM course WHERE teacher_id = #{tid} AND deleted = 0")
    long countCoursesByTeacher(@Param("tid") long tid);

    @Select("SELECT id, code, name, category, credits, campus, place, assessment, capacity, enrolled, remaining, "
            + "rating, prereq_name AS prereq, intro FROM course WHERE teacher_id = #{tid} AND deleted = 0 ORDER BY id")
    List<Map<String, Object>> coursesByTeacher(@Param("tid") long tid);

    /* ==================== 选课记录 ==================== */

    @Select("SELECT sc.course_id AS courseId, c.code AS code, c.name AS name, c.credits AS credits, "
            + "c.category AS category, c.teacher_name AS teacher, c.campus AS campus, c.place AS place, "
            + "c.assessment AS assessment, c.capacity AS capacity, c.enrolled AS enrolled, "
            + "c.remaining AS remaining, c.rating AS rating "
            + "FROM student_course sc JOIN course c ON c.id = sc.course_id "
            + "WHERE sc.student_id = #{sid} AND sc.status = 'SELECTED' AND c.deleted = 0 ORDER BY sc.id")
    List<Map<String, Object>> enrolledCourses(@Param("sid") long sid);

    @Select("SELECT COUNT(*) FROM student_course WHERE student_id = #{sid} AND course_id = #{cid} AND status = 'SELECTED'")
    int isEnrolled(@Param("sid") long sid, @Param("cid") long cid);

    @Select("SELECT COUNT(*) FROM student_course WHERE student_id = #{sid} AND status = 'SELECTED'")
    long countEnrolledByStudent(@Param("sid") long sid);

    @Insert("INSERT INTO student_course (student_id, course_id, status, source) "
            + "VALUES (#{sid}, #{cid}, 'SELECTED', #{source}) "
            + "ON DUPLICATE KEY UPDATE status = 'SELECTED', source = #{source}, selected_at = NOW(), dropped_at = NULL")
    int enroll(@Param("sid") long sid, @Param("cid") long cid, @Param("source") String source);

    @Update("UPDATE student_course SET status = 'DROPPED', dropped_at = NOW() "
            + "WHERE student_id = #{sid} AND course_id = #{cid} AND status = 'SELECTED'")
    int dropCourse(@Param("sid") long sid, @Param("cid") long cid);

    @Select("SELECT COUNT(*) FROM student_course WHERE status = 'SELECTED'")
    long countSelectedAll();

    @Select("SELECT s.student_no AS studentNo, u.real_name AS name, s.grade, s.major, sc.status AS status "
            + "FROM student_course sc JOIN student s ON s.id = sc.student_id JOIN sys_user u ON u.id = s.user_id "
            + "WHERE sc.course_id = #{cid} AND sc.status = 'SELECTED' ORDER BY s.student_no")
    List<Map<String, Object>> roster(@Param("cid") long cid);

    /* ==================== 心愿单 ==================== */

    @Select("SELECT w.course_id AS courseId, w.priority AS priority FROM wishlist w "
            + "WHERE w.student_id = #{sid} ORDER BY w.priority, w.id")
    List<Map<String, Object>> wishlist(@Param("sid") long sid);

    @Select("SELECT COUNT(*) FROM wishlist WHERE student_id = #{sid} AND course_id = #{cid}")
    int inWishlist(@Param("sid") long sid, @Param("cid") long cid);

    @Select("SELECT COUNT(*) FROM wishlist WHERE student_id = #{sid}")
    int wishlistSize(@Param("sid") long sid);

    @Select("SELECT IFNULL(MAX(priority), 0) FROM wishlist WHERE student_id = #{sid}")
    int maxPriority(@Param("sid") long sid);

    @Insert("INSERT INTO wishlist (student_id, course_id, priority) VALUES (#{sid}, #{cid}, #{priority})")
    int wishlistAdd(@Param("sid") long sid, @Param("cid") long cid, @Param("priority") int priority);

    @Delete("DELETE FROM wishlist WHERE student_id = #{sid} AND course_id = #{cid}")
    int wishlistRemove(@Param("sid") long sid, @Param("cid") long cid);

    @Update("UPDATE wishlist SET priority = #{priority} WHERE student_id = #{sid} AND course_id = #{cid}")
    int wishlistSetPriority(@Param("sid") long sid, @Param("cid") long cid, @Param("priority") int priority);

    @Delete("DELETE FROM wishlist WHERE student_id = #{sid}")
    int wishlistClear(@Param("sid") long sid);

    @Select("SELECT COUNT(*) FROM wishlist")
    long countWishlistAll();

    /* ==================== 放号订阅 ==================== */

    @Select("SELECT ss.course_id AS courseId FROM seat_subscription ss "
            + "WHERE ss.student_id = #{sid} AND ss.status <> 'CANCELLED' ORDER BY ss.id")
    List<Map<String, Object>> subscriptions(@Param("sid") long sid);

    @Insert("INSERT INTO seat_subscription (student_id, course_id, status) VALUES (#{sid}, #{cid}, 'ACTIVE') "
            + "ON DUPLICATE KEY UPDATE status = 'ACTIVE'")
    int subscribe(@Param("sid") long sid, @Param("cid") long cid);

    @Update("UPDATE seat_subscription SET status = 'CANCELLED' WHERE student_id = #{sid} AND course_id = #{cid}")
    int unsubscribe(@Param("sid") long sid, @Param("cid") long cid);

    @Select("SELECT u.id AS userId, u.real_name AS name FROM seat_subscription ss "
            + "JOIN student s ON s.id = ss.student_id JOIN sys_user u ON u.id = s.user_id "
            + "WHERE ss.course_id = #{cid} AND ss.status = 'ACTIVE'")
    List<Map<String, Object>> activeSubscriberUsers(@Param("cid") long cid);

    @Update("UPDATE seat_subscription SET status = 'NOTIFIED' WHERE course_id = #{cid} AND status = 'ACTIVE'")
    int markNotified(@Param("cid") long cid);

    /* ==================== 选课受理票据 ==================== */

    @Insert("INSERT INTO selection_ticket (ticket_no, student_id, status, queue_position, total_count, "
            + "accepted_count, rejected_count) VALUES (#{ticketNo}, #{sid}, 'ACCEPTED', #{queuePos}, #{total}, 0, 0)")
    int createTicket(@Param("ticketNo") String ticketNo, @Param("sid") long sid,
                     @Param("queuePos") int queuePos, @Param("total") int total);

    @Select("SELECT id FROM selection_ticket WHERE ticket_no = #{ticketNo} LIMIT 1")
    Long ticketIdByNo(@Param("ticketNo") String ticketNo);

    @Update("UPDATE selection_ticket SET status = #{status} WHERE id = #{id}")
    int updateTicketStatus(@Param("id") long id, @Param("status") String status);

    @Update("UPDATE selection_ticket SET status = #{status}, accepted_count = #{accepted}, "
            + "rejected_count = #{rejected}, finished_at = NOW() WHERE id = #{id}")
    int finishTicket(@Param("id") long id, @Param("status") String status,
                     @Param("accepted") int accepted, @Param("rejected") int rejected);

    @Select("SELECT id, ticket_no AS ticketNo, status, queue_position AS queuePos, total_count AS total, "
            + "accepted_count AS accepted, rejected_count AS rejected, submitted_at AS submittedAt "
            + "FROM selection_ticket WHERE ticket_no = #{ticketNo} LIMIT 1")
    Map<String, Object> ticketByNo(@Param("ticketNo") String ticketNo);

    @Select("SELECT ticket_no AS id, status, accepted_count AS accepted, rejected_count AS rejected, "
            + "total_count AS total, submitted_at AS submittedAt FROM selection_ticket ORDER BY id DESC LIMIT 6")
    List<Map<String, Object>> recentTickets();

    @Select("SELECT COUNT(*) FROM selection_ticket")
    long countTickets();

    @Insert("INSERT INTO selection_ticket_item (ticket_id, course_id, course_name, result, reason) "
            + "VALUES (#{ticketId}, #{courseId}, #{courseName}, #{result}, #{reason})")
    int addTicketItem(@Param("ticketId") long ticketId, @Param("courseId") long courseId,
                      @Param("courseName") String courseName, @Param("result") String result,
                      @Param("reason") String reason);

    @Select("SELECT course_id AS courseId, course_name AS courseName, result, reason "
            + "FROM selection_ticket_item WHERE ticket_id = #{ticketId} ORDER BY id")
    List<Map<String, Object>> ticketItems(@Param("ticketId") long ticketId);

    /* ==================== 消息中心 ==================== */

    @Select("SELECT id, type, title, body, is_read AS isRead, created_at AS createdAt "
            + "FROM message WHERE user_id = #{userId} ORDER BY id DESC")
    List<Map<String, Object>> messages(@Param("userId") long userId);

    @Update("UPDATE message SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    int markMessagesRead(@Param("userId") long userId);

    @Insert("INSERT INTO message (user_id, type, title, body) VALUES (#{userId}, #{type}, #{title}, #{body})")
    int addMessage(@Param("userId") long userId, @Param("type") String type,
                   @Param("title") String title, @Param("body") String body);

    /* ==================== 个人偏好 ==================== */

    @Insert("INSERT INTO user_preference (user_id) VALUES (#{userId}) ON DUPLICATE KEY UPDATE user_id = user_id")
    int ensurePreference(@Param("userId") long userId);

    @Select("SELECT theme, density, font_scale AS fontScale, timetable_view AS timetableView, "
            + "compact_filter AS compactFilter, notify_result AS notifyResult, notify_seat AS notifySeat, "
            + "notify_system AS notifySystem, notify_drop AS notifyDrop "
            + "FROM user_preference WHERE user_id = #{userId}")
    Map<String, Object> preference(@Param("userId") long userId);

    @Update("UPDATE user_preference SET theme = #{theme}, density = #{density}, font_scale = #{fontScale}, "
            + "timetable_view = #{timetableView}, compact_filter = #{compactFilter}, "
            + "notify_result = #{notifyResult}, notify_seat = #{notifySeat}, "
            + "notify_system = #{notifySystem}, notify_drop = #{notifyDrop} WHERE user_id = #{userId}")
    int updatePreference(@Param("userId") long userId, @Param("theme") String theme,
                         @Param("density") String density, @Param("fontScale") String fontScale,
                         @Param("timetableView") String timetableView, @Param("compactFilter") int compactFilter,
                         @Param("notifyResult") int notifyResult, @Param("notifySeat") int notifySeat,
                         @Param("notifySystem") int notifySystem, @Param("notifyDrop") int notifyDrop);

    /* ==================== 教务规则 ==================== */

    @Select("SELECT selection_open AS selectionOpen, credit_limit AS creditLimit, max_wishlist AS maxWishlist, "
            + "allow_cross_campus AS allowCrossCampus, block_on_conflict AS blockOnConflict "
            + "FROM selection_rule WHERE id = 1")
    Map<String, Object> rules();

    @Update("UPDATE selection_rule SET selection_open = #{selectionOpen}, credit_limit = #{creditLimit}, "
            + "max_wishlist = #{maxWishlist}, allow_cross_campus = #{allowCrossCampus}, "
            + "block_on_conflict = #{blockOnConflict} WHERE id = 1")
    int updateRules(@Param("selectionOpen") int selectionOpen, @Param("creditLimit") double creditLimit,
                    @Param("maxWishlist") int maxWishlist, @Param("allowCrossCampus") int allowCrossCampus,
                    @Param("blockOnConflict") int blockOnConflict);

    /* ==================== 异常工单 ==================== */

    @Select("SELECT a.id, a.type, a.student_no AS studentNo, a.student_name AS studentName, "
            + "a.course_id AS courseId, a.course_name AS courseName, a.reason, a.status, a.resolution, "
            + "a.created_at AS createdAt FROM anomaly_ticket a ORDER BY a.id DESC")
    List<Map<String, Object>> allAnomalies();

    @Select("SELECT a.id, a.type, a.student_no AS studentNo, a.student_name AS studentName, "
            + "a.course_id AS courseId, a.course_name AS courseName, a.reason, a.status, a.resolution, "
            + "a.created_at AS createdAt FROM anomaly_ticket a WHERE a.status = #{status} ORDER BY a.id DESC")
    List<Map<String, Object>> anomaliesByStatus(@Param("status") String status);

    @Select("SELECT COUNT(*) FROM anomaly_ticket WHERE status = 'PENDING'")
    long countPendingAnomalies();

    @Select("SELECT a.id, a.type, a.student_no AS studentNo, a.student_name AS studentName, "
            + "a.course_id AS courseId, a.course_name AS courseName, a.reason, a.status, a.resolution "
            + "FROM anomaly_ticket a WHERE a.id = #{id}")
    Map<String, Object> anomalyById(@Param("id") long id);

    @Update("UPDATE anomaly_ticket SET status = #{status}, resolution = #{resolution}, "
            + "handled_by = #{by}, handled_at = NOW() WHERE id = #{id}")
    int resolveAnomaly(@Param("id") long id, @Param("status") String status,
                       @Param("resolution") String resolution, @Param("by") String by);

    /* ==================== 学生档案 / 名额快照 / 数据重置（补充） ==================== */

    @Select("SELECT s.id AS studentId, s.student_no AS studentNo, s.user_id AS userId, u.real_name AS name, "
            + "s.grade, s.major, s.credit_limit AS creditLimit FROM student s JOIN sys_user u ON u.id = s.user_id "
            + "WHERE s.id = #{sid} AND s.deleted = 0 LIMIT 1")
    Map<String, Object> studentProfileById(@Param("sid") long sid);

    @Select("SELECT s.id AS studentId, s.student_no AS studentNo, s.user_id AS userId, u.real_name AS name, "
            + "s.grade, s.major, s.credit_limit AS creditLimit FROM student s JOIN sys_user u ON u.id = s.user_id "
            + "WHERE s.deleted = 0 ORDER BY s.student_no")
    List<Map<String, Object>> allStudents();

    @Select("SELECT id FROM student WHERE student_no = #{no} AND deleted = 0 LIMIT 1")
    Long studentIdByNo(@Param("no") String no);

    @Select("SELECT sc.course_id AS courseId FROM student_course sc "
            + "WHERE sc.student_id = #{sid} AND sc.status = 'SELECTED' ORDER BY sc.id")
    List<Map<String, Object>> selectedCourseIds(@Param("sid") long sid);

    @Select("SELECT id, capacity, enrolled, remaining FROM course WHERE deleted = 0 ORDER BY id")
    List<Map<String, Object>> seatRows();

    @Update("UPDATE course SET enrolled = enrolled + 1 WHERE id = #{id} AND deleted = 0")
    int forceTakeSeat(@Param("id") long id);

    @Update("UPDATE course SET enrolled = #{n} WHERE id = #{id} AND deleted = 0")
    int setEnrolled(@Param("id") long id, @Param("n") int n);

    @Delete("DELETE FROM selection_ticket")
    int deleteAllTickets();

    @Delete("DELETE FROM student_course WHERE source <> 'SEED'")
    int deleteNonSeedEnrollments();

    @Delete("DELETE FROM wishlist")
    int deleteAllWishlist();

    @Delete("DELETE FROM seat_subscription")
    int deleteAllSubscriptions();

    @Update("UPDATE anomaly_ticket SET status = 'PENDING', resolution = NULL, handled_by = NULL, handled_at = NULL")
    int resetAnomalies();
}
