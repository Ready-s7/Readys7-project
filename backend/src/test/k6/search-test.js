import http from 'k6/http';
import { sleep, check } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 500 },
        { duration: '30s', target: 1000 },
        { duration: '30s', target: 1500 },
        { duration: '30s', target: 2000 },
        { duration: '30s', target: 0 },
    ]

};

export default function () {
    const keywords = ['Java', 'Spring', 'React', 'Python', 'Docker'];
    const keyword = keywords[Math.floor(Math.random() * keywords.length)];

    const res = http.get(`http://localhost:8080/api/v2/search?keyword=${keyword}`);

    check(res, {
        'status is 200': (r) => r.status === 200,
    });

    sleep(1);
}