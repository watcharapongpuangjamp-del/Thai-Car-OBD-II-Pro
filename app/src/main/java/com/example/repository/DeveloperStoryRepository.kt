package com.example.repository

import com.example.model.developer.*

object DeveloperStoryRepository {

    val developerProfile = DeveloperProfile(
        name = "GigaTV Thai (AI Studio Developer)",
        role = "Lead Developer & Architect",
        contactUrl = "gigatvthai@gmail.com"
    )

    val developerStatement = DeveloperStatement(
        whyCreatedTh = "สร้างขึ้นเพื่อลดข้อจำกัดในการเข้าถึงข้อมูลวิเคราะห์รถยนต์เชิงลึกที่มักมีราคาสูง หรือเข้าถึงได้ยากสำหรับผู้ใช้ทั่วไป โปรเจกต์นี้ตั้งใจให้คนไทยมีเครื่องมือวินิจฉัยรถยนต์เป็นภาษาไทยที่แม่นยำ เทียบเท่าเครื่องมือระดับมืออาชีพ",
        problemToSolveTh = "ปัญหาการถูกหลอกซ่อม หรือการประเมินอาการรถที่คลาดเคลื่อนโดยไม่มีข้อมูลอ้างอิง แอปนี้จึงออกแบบมาให้อ่านค่าจาก ECU โดยตรงเพื่อเป็นหลักฐานความจริง (Evidence-based)",
        corePrinciplesTh = "1. ความจริงต้องมาก่อน (Real Hardware First) ไม่มีการเมคข้อมูล\n2. ความโปร่งใสของข้อมูล (Data Provenance) ต้องบอกได้ว่าข้อมูลมาจากไหน\n3. ต้องใช้งานง่ายแต่เจาะลึกได้ (Simple UI, Deep Logic)",
        futureVisionTh = "ต้องการให้ Thai Car OBD-II Pro เป็นมาตรฐานใหม่ของการดูแลรถยนต์ด้วยตัวเอง และเป็นสะพานเชื่อมระหว่างช่างและผู้ใช้รถผ่าน AI Mechanic ที่มีประสิทธิภาพ"
    )

    val developerFeelings = DeveloperFeelings(
        feelingAtStartTh = "เริ่มต้นด้วยความตื่นเต้นและกังวล เพราะการเชื่อมต่อ Hardware (ELM327) กับระบบ Android เป็นเรื่องท้าทาย และต้องรองรับรถยนต์หลากหลายแบรนด์ที่ใช้โปรโตคอลต่างกัน",
        challengesTh = "การจัดการ Asynchronous Data Flow จาก Serial Port ที่มีความเร็วและข้อจำกัดของบัฟเฟอร์ ไปจนถึงการจับคู่ Data Stream ให้ตรงกับจังหวะการดึงข้อมูลเพื่อไม่ให้ค้าง (Freeze)",
        mistakesTh = "เคยใช้ระบบ Polling แบบอิงจำนวนรอบ (Cycle-based) ซึ่งทำให้เกิดปัญหาความหน่วงและ Timeout บ่อยครั้งเมื่อดึงข้อมูลพร้อมกันหลายเซนเซอร์",
        lessonsLearnedTh = "เรียนรู้ว่า Architecture ที่ดีคือการแยก Layer ของ Hardware ออกจาก UI อย่างเด็ดขาด และการใช้ Time-based scheduler ดีกว่ารอบการทำงานตายตัว",
        proudestMomentsTh = "เมื่อสามารถออกแบบ Dynamic ECU Discovery ได้สำเร็จ ทำให้สแกน DTC ได้ครบทุกโมดูล (ECM, TCM, ABS) โดยไม่ต้องเดา Header ล่วงหน้า และระบบ AI ที่ทำงานคู่กับ Rule Engine อย่างลงตัว",
        unfinishedThingsTh = "การพัฒนาระบบ Cloud Sync ข้ามอุปกรณ์แบบ Real-time และระบบ Security ที่แน่นหนาขึ้นสำหรับ API Gateway ที่ยังคงต้องใช้เวลาขัดเกลา",
        hopesTh = "หวังว่าโปรเจกต์นี้จะเป็นเครื่องมือคู่ใจของช่างไทยและคนรักรถทุกคน และช่วยประหยัดค่าซ่อมบำรุงในสิ่งที่ไม่จำเป็น"
    )

    val journalEntries = mutableListOf(
        DeveloperJournalEntry(
            date = "2023-10-15",
            version = "v0.1.0",
            eventTh = "เริ่มต้นโปรเจกต์ (Kickoff)",
            currentlyDoingTh = "ศึกษาโปรโตคอล ELM327 และการสื่อสารผ่าน USB Serial",
            problemsFoundTh = "เอกสาร ELM327 ค่อนข้างเก่าและการทำงานจริงบนรถบางรุ่นไม่ได้มาตรฐาน",
            lessonsTh = "ต้องมีระบบ Simulator เพื่อจำลองการทำงานก่อนไปทดสอบกับรถจริงทุกครั้ง",
            developerFeelingsTh = "รู้สึกท้าทายและเต็มไปด้วยไฟ! นี่คือจุดเริ่มต้นของการสร้างสิ่งที่ยิ่งใหญ่",
            isPinned = true
        ),
        DeveloperJournalEntry(
            date = "2024-02-10",
            version = "v1.2.0",
            eventTh = "เจอปัญหาการดึงค่าเซนเซอร์ค้าง",
            currentlyDoingTh = "เขียนระบบ Live Telemetry สำหรับ Dashboard",
            problemsFoundTh = "UI ค้างเมื่อส่งคำสั่งถี่ยาวนานเกินไป (Buffer Overflow)",
            lessonsTh = "Coroutines ใน Android ต้องใช้ Dispatchers.IO แยกออกให้ชัดเจน และควรมี Timeout ให้แต่ละคำสั่ง",
            developerFeelingsTh = "เครียดเล็กน้อย นั่งแก้บั๊กข้ามคืน แต่พอรู้ว่าปัญหาคือ Threading ก็โล่งใจ",
            isPinned = false
        ),
        DeveloperJournalEntry(
            date = "2024-08-19",
            version = "v2.5.0",
            eventTh = "ยกเครื่อง Architecture ครั้งใหญ่",
            currentlyDoingTh = "แก้ระบบ Polling เป็น Time-based และทำ Dynamic ECU Discovery",
            problemsFoundTh = "การรวมเอาตรรกะเก่าและใหม่เข้าด้วยกันทำให้เกิดความสับสนระหว่าง Real และ Simulator",
            lessonsTh = "การกำหนด Data Provenance (แหล่งที่มาข้อมูล) เป็นหัวใจสำคัญของแอปข้อมูลวินิจฉัย ห้ามผสมข้อมูลจริงกับข้อมูลจำลองเด็ดขาด",
            developerFeelingsTh = "ภูมิใจมากที่รื้อโค้ดและจัดวางระบบใหม่จนเสถียร นี่คือเวอร์ชัน Pro อย่างแท้จริง",
            isPinned = true
        )
    )

    val technicalMilestones = listOf(
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
    )

    fun addJournalEntry(entry: DeveloperJournalEntry) {
        journalEntries.add(0, entry) // Add to top
    }
}
