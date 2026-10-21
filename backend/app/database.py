from sqlalchemy import create_engine
from sqlalchemy.orm import declarative_base, sessionmaker
from app.config import settings

def _build_db_url(url: str) -> str:
    """
    Supabase gives: postgres://user:pass@host:5432/db
    SQLAlchemy needs: postgresql+psycopg2://user:pass@host:5432/db
    """
    if url.startswith("postgres://"):
        url = url.replace("postgres://", "postgresql+psycopg2://", 1)
    elif url.startswith("postgresql://") and "+psycopg2" not in url:
        url = url.replace("postgresql://", "postgresql+psycopg2://", 1)
    return url

_db_url = _build_db_url(settings.DATABASE_URL)

# SQLite requires check_same_thread=False for multithreaded FastAPI requests
connect_args = {}
if _db_url.startswith("sqlite"):
    connect_args = {"check_same_thread": False}

engine = create_engine(
    _db_url,
    connect_args=connect_args,
    # Connection pool tuning for Render free tier (limits concurrent connections)
    pool_pre_ping=True,
    pool_size=5,
    max_overflow=10,
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()

def get_db():
    """Dependency to provide a database session per request."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
