from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime


def extract():
    return "s3://bucket/raw/orders.json"


def transform(**context):
    raw_uri = context["ti"].xcom_pull(task_ids="extract")
    return raw_uri.replace("/raw/", "/clean/")


def load(**context):
    clean_uri = context["ti"].xcom_pull(task_ids="transform")
    print(f"loading {clean_uri}")


with DAG(
    dag_id="orders_etl",
    start_date=datetime(2026, 1, 1),
    schedule="@daily",
    catchup=False,
) as dag:
    extract_task = PythonOperator(task_id="extract", python_callable=extract)
    transform_task = PythonOperator(task_id="transform", python_callable=transform)
    load_task = PythonOperator(task_id="load", python_callable=load)

    extract_task >> transform_task >> load_task

