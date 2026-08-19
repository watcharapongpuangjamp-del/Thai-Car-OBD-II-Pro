with open('./app/src/main/java/com/example/repository/DeveloperStoryRepository.kt', 'r') as f:
    content = f.read()

import re
old_milestones = re.search(r'val technicalMilestones = listOf\(.*?\)', content, re.DOTALL).group(0)

new_milestones = '''val technicalMilestones = listOf(
        TechnicalMilestone(
            category = "Architecture",
            titleTh = "MVVM + Clean Architecture",
            descriptionTh = "ปรับโครงสร้างแยก Hardware, Repository และ UI ออกจากกันอย่างเด็ดขาด",
            achievedVersion = "v1.0.0",
            date = "2023-11-20"
        ),
        TechnicalMilestone(
            category = "ELM327",
            titleTh = "ELM327 Native Driver",
            descriptionTh = "เขียน Driver ควบคุม ELM327 โดยตรงผ่าน Serial USB แทนการใช้ Library สำเร็จรูป",
            achievedVersion = "v1.2.0",
            date = "2023-12-05"
        ),
        TechnicalMilestone(
            category = "OBD Protocol",
            titleTh = "Strict Mode 03/07/0A/04 Validation",
            descriptionTh = "บังคับให้ตรวจสอบโปรโตคอลมาตรฐาน ป้องกันการใช้ข้อมูลปลอมมาหลอกลวง",
            achievedVersion = "v2.5.0",
            date = "2024-07-10"
        ),
        TechnicalMilestone(
            category = "ECU Discovery",
            titleTh = "Dynamic Header Resolution",
            descriptionTh = "แอปสามารถค้นหา Response Header ของ ECU ได้เอง (เช่น 7E8, 7E9) ไม่เดาค่า",
            achievedVersion = "v2.6.0",
            date = "2024-08-19"
        ),
        TechnicalMilestone(
            category = "Telemetry",
            titleTh = "Time-based Adaptive Polling",
            descriptionTh = "เปลี่ยนการดึงข้อมูลตามรอบเป็นตามเวลาจริง (Fast/Med/Slow) ป้องกัน UI ค้าง",
            achievedVersion = "v2.5.0",
            date = "2024-08-10"
        ),
        TechnicalMilestone(
            category = "DTC",
            titleTh = "Real-time Fault Code Parsing",
            descriptionTh = "ระบบดึงข้อมูลรหัสวิเคราะห์ปัญหา (DTC) ตรงจาก ECU โดยไม่มีการสมมติ",
            achievedVersion = "v1.5.0",
            date = "2024-02-15"
        ),
        TechnicalMilestone(
            category = "Evidence Chain",
            titleTh = "ValidatedDiagnosticSnapshot",
            descriptionTh = "เก็บประวัติคำสั่ง (Raw TX/RX) ทุกครั้งที่มีการสแกน เพื่อใช้เป็นหลักฐานอ้างอิงยืนยันความจริง",
            achievedVersion = "v2.6.0",
            date = "2024-08-19"
        ),
        TechnicalMilestone(
            category = "Replay Engine",
            titleTh = "ReplayElm327Driver",
            descriptionTh = "ระบบ Simulator ที่เล่นข้อมูลประวัติจากรถจริงซ้ำได้เหมือนต่อ Hardware จริง",
            achievedVersion = "v2.6.0",
            date = "2024-08-18"
        ),
        TechnicalMilestone(
            category = "AI",
            titleTh = "AI Mechanic + Rule Engine",
            descriptionTh = "เชื่อมต่อ Gemini AI เข้ากับกฎวินิจฉัยเพื่ออธิบายผลเป็นภาษาไทยแบบเข้าใจง่ายและโปร่งใส",
            achievedVersion = "v2.0.0",
            date = "2024-05-15"
        ),
        TechnicalMilestone(
            category = "Real Hardware",
            titleTh = "Real Hardware Source Isolation",
            descriptionTh = "แยกชั้นข้อมูลระหว่าง Hardware จริงกับข้อมูลจำลอง (Simulator) ออกจากกัน 100% ไม่ยอมให้ผสมเด็ดขาด",
            achievedVersion = "v2.6.0",
            date = "2024-08-19"
        )
    )'''

content = content.replace(old_milestones, new_milestones)

with open('./app/src/main/java/com/example/repository/DeveloperStoryRepository.kt', 'w') as f:
    f.write(content)
