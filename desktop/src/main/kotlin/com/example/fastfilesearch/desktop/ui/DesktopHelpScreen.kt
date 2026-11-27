package com.example.fastfilesearch.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DesktopHelpScreen() {
    val colors = LocalDesktopColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 860.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "المساعدة ودليل الاستخدام",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "مرجع سريع لاختصارات لوحة المفاتيح والتعامل مع الجداول والنسخ والتصدير.",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }

            HelpSectionCard(
                title = "اختصارات لوحة المفاتيح",
                icon = Icons.Outlined.Keyboard
            ) {
                ShortcutRow("Ctrl + F", "التركيز الفوري على مربع البحث")
                HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                ShortcutRow("F5", "تحديث الفهرس للمجلد النشط أو جميع المجلدات")
                HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                ShortcutRow("F1", "فتح شاشة المساعدة الحالية")
                HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                ShortcutRow("Ctrl + C", "نسخ الخلايا أو الصفوف المحددة تلقائياً")
                HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                ShortcutRow("Ctrl + A", "تحديد جميع الخلايا في الورقة الحالية أو النتائج")
                HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                ShortcutRow("Enter", "فتح نافذة المعاينة عند تحديد خلية واحدة أو صف واحد")
                HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                ShortcutRow("Alt + Enter", "إظهار أو إخفاء لوحة التفاصيل الجانبية")
                HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                ShortcutRow("Esc", "إلغاء التحديد الحالي")
                HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                ShortcutRow("Ctrl + L", "قفل التطبيق فوراً عند تفعيل القفل")
                HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                ShortcutRow("أسهم الاتجاه", "التنقل بين خلايا الجدول وتمرير العرض تلقائياً")
            }

            HelpSectionCard(
                title = "تحديد الخلايا والصفوف بالماوس",
                icon = Icons.Outlined.Mouse
            ) {
                HelpBullet("النقر على خلية بيانات يحدد الخلية، والنقر المزدوج يفتح نافذة معاينة النص.")
                HelpBullet("النقر على خانة رقم الصف (يمين الجدول) يحدد الصف كاملاً مباشرة.")
                HelpBullet("لتحديد عدة خلايا أو عدة صفوف متفرقة، اضغط مع الاستمرار على مفتاح Ctrl أثناء النقر.")
                HelpBullet("النقر بزر الفأرة الأيمن يفتح قائمة سريعة (نسخ، معاينة عند تحديد عنصر واحد، تصدير، فتح الملف، موقع الملف).")
                HelpBullet("لتغيير عرض أي عمود، اسحب الحد الفاصل لرأس العمود، وانقر نقراً مزدوجاً عليه لإعادته للعرض التلقائي.")
            }

            HelpSectionCard(
                title = "الفرز والتصفية من رأس العمود",
                icon = Icons.Outlined.FilterList
            ) {
                HelpBullet("انقر على أيقونة السهم في رأس أي عمود لفتح قائمة الفرز والتصفية الخاصة به.")
                HelpBullet("يدعم الفرز «من أ إلى ي» و«من ي إلى أ» الأرقام حسابياً والنصوص العربية بالتطبيع، مع إبقاء القيم الفارغة في الأسفل دائماً.")
                HelpBullet("يمكنك البحث داخل قيم العمود وتحديد أو إلغاء تحديد القيم الفريدة المتوفرة ثم الضغط على «موافق».")
                HelpBullet("استخدم «إلغاء عامل التصفية» لإعادة العمود لحالته الأصلية.")
            }

            HelpSectionCard(
                title = "النسخ والتصدير التلقائي حسب السياق",
                icon = Icons.Outlined.TableChart
            ) {
                HelpBullet("زر «نسخ» وزر «تصدير» يعملان تلقائياً حسب ما حددته: خلية أو عدة خلايا، صف أو عدة صفوف (مع أسماء الأعمدة)، أو جميع النتائج المعروضة عند عدم التحديد.")
                HelpBullet("تظهر أزرار صيغ الملفات لأنواع الملفات المتوفرة فعلياً في نتائج البحث أو الفهرس.")
                HelpBullet("عند كثرة النتائج داخل ملف، تُعرض دفعة أولية متوازنة مع زر «عرض المزيد» داخل البطاقة لتحميل بقية الصفوف دون حجب الملفات الأخرى.")
            }
        }
    }
}

@Composable
private fun HelpSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    val colors = LocalDesktopColors.current
    DesktopSectionCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.controlActiveBackground,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
            content()
        }
    }
}

@Composable
private fun ShortcutRow(shortcut: String, description: String) {
    val colors = LocalDesktopColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = description,
            fontSize = 13.5.sp,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(colors.subCardBackground)
                .border(1.dp, colors.divider, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = shortcut,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = colors.textPrimary
            )
        }
    }
}

@Composable
private fun HelpBullet(text: String) {
    val colors = LocalDesktopColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("•", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.controlActiveBackground)
        Text(text, fontSize = 13.5.sp, lineHeight = 20.sp, color = colors.textPrimary)
    }
}
