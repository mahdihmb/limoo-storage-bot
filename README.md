<div dir="rtl">

## بات مخزن
ابزاری است برای ذخیره و مدیریت پیام‌ها در [لیمو](https://web.limoo.im/). با این بات می‌توانید به راحتی به پیام‌های ذخیره شده دسترسی داشته باشید بدون اینکه در بین پیام‌های گروه‌ها و رشته‌ها سرگردان شوید.  
امکان ذخیره پیام‌های داخل رشته و گروه - امکان ذخیره پیام به همراه ضمایم آن - امکان جستجو در اسامی پیام‌ها - امکان دریافت پیام با کلیدواژه موجود در اسم آن - مخزن جدا برای فضای کاری (علاوه بر مخزن شخصی)
***
### تکنولوژي‌های مورد نیاز:
- java
- postgresql
***
### راه‌اندازی بات:
1. یک دیتابیس جدید ایجاد کنید (مثلا با نام `LimooStorageBot`)
2. یک فایل `config.properties` در یک جایی از سیستم ایجاد کرده و آدرس آن را در متغیر محیطی `LIMOO_STORAGE_BOT_CONFIG` قرار دهید. یا به سادگی فقط فایل config.properties موجود در کد را تغییر دهید (به جای ایجاد کانفیگ لوکال)
3. در فایل `config.properties` که ایجاد کرده‌اید (یا فایل کانفیگ موجود در کد)، موارد زیر را تنظیم کنید:
</div>

```properties
# Bot
bot.limooUrl=https://web.limoo.im/Limonad
bot.username=bot_username
bot.password=bot_password

# DB
db.host=localhost
db.port=5432
db.name=LimooStorageBot
db.username=db_username
db.password=db_password
```

<div dir="rtl">

4. اگر از لینوکس استفاده می‌کنید، ابتدا دستور زیر را اجرا کنید:
</div>

```bash
chmod +x gradlew
```

<div dir="rtl">

5. دستور زیر را برای پیکربندی دیتابیس اجرا کنید (این دستور فقط یکبار باید اجرا شود):
</div>

```bash
./gradlew resetAndInitDB
```

<div dir="rtl">

6. دستور زیر را برای شروع به کار بات اجرا کنید:
</div>

```bash
./gradlew runBot
```

<div dir="rtl">

همچنین می‌توانید به جای استفاده از تسک gradle بالا برای اجرای بات، با دستور
</div>

```bash
./gradlew jar
```

<div dir="rtl">

یک جر از بات خود بسازید (که در مسیر build/libs ایجاد می‌شود) و سپس با دستور زیر آن را اجرا کنید:
</div>

```bash
java -jar limoo-storage-bot.jar
```
