package com.courseplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 账号偏好设置 */
@TableName("user_preference")
public class UserPreference {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String theme;
    private String density;
    private String fontScale;
    private String timetableView;
    private Integer compactFilter;
    private Integer notifyResult;
    private Integer notifySeat;
    private Integer notifySystem;
    private Integer notifyDrop;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public String getDensity() { return density; }
    public void setDensity(String density) { this.density = density; }
    public String getFontScale() { return fontScale; }
    public void setFontScale(String fontScale) { this.fontScale = fontScale; }
    public String getTimetableView() { return timetableView; }
    public void setTimetableView(String timetableView) { this.timetableView = timetableView; }
    public Integer getCompactFilter() { return compactFilter; }
    public void setCompactFilter(Integer compactFilter) { this.compactFilter = compactFilter; }
    public Integer getNotifyResult() { return notifyResult; }
    public void setNotifyResult(Integer notifyResult) { this.notifyResult = notifyResult; }
    public Integer getNotifySeat() { return notifySeat; }
    public void setNotifySeat(Integer notifySeat) { this.notifySeat = notifySeat; }
    public Integer getNotifySystem() { return notifySystem; }
    public void setNotifySystem(Integer notifySystem) { this.notifySystem = notifySystem; }
    public Integer getNotifyDrop() { return notifyDrop; }
    public void setNotifyDrop(Integer notifyDrop) { this.notifyDrop = notifyDrop; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
