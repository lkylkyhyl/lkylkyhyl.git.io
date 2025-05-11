const lotteryBtn = document.querySelector('.lottery-btn');
const lotteryItems = document.querySelectorAll('.lottery-item');

// 筛选出包含指定奖品的抽奖项
const targetLotteryItems = Array.from(lotteryItems).filter(item => {
    const imgSrc = item.querySelector('img').src;
    return ['gift_1', 'gift_2', 'gift_3', 'gift_4'].some(prize => imgSrc.includes(prize));
});

lotteryBtn.addEventListener('click', function () {
    const randomIndex = Math.floor(Math.random() * targetLotteryItems.length);
    lotteryItems.forEach(item => {
        item.style.backgroundColor = '#fff';
    });
    const winningItem = targetLotteryItems[randomIndex];
    winningItem.style.backgroundColor = 'lightgreen';

    // 延迟 1 秒跳转到中奖页面
    setTimeout(() => {
        window.location.href = '中奖页面.html';
    }, 1000);
});
